/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopException;
import io.netty.channel.SelectStrategy;
import io.netty.channel.SingleThreadEventLoop;
import io.netty.util.IntSupplier;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ReflectionUtil;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * todo NioEventLoop维护Selector   它实现了,将一个Channel注册进Selector
 * {@link SingleThreadEventLoop} implementation which register the {@link Channel}'s to a
 * {@link Selector} and so does the multi-plexing of these in the event loop.
 */
public final class NioEventLoop extends SingleThreadEventLoop {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioEventLoop.class);

    private static final int CLEANUP_INTERVAL = 256; // XXX Hard-coded value, but won't need customization.

    private static final boolean DISABLE_KEYSET_OPTIMIZATION =
            SystemPropertyUtil.getBoolean("io.netty.noKeySetOptimization", false);

    private static final int MIN_PREMATURE_SELECTOR_RETURNS = 3;
    private static final int SELECTOR_AUTO_REBUILD_THRESHOLD;

    private final IntSupplier selectNowSupplier = new IntSupplier() {
        @Override
        public int get() throws Exception {
            return selectNow();
        }
    };
    private final Callable<Integer> pendingTasksCallable = new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            return NioEventLoop.super.pendingTasks();
        }
    };

    // Workaround for JDK NIO bug.
    //
    // See:
    // - http://bugs.sun.com/view_bug.do?bug_id=6427854
    // - https://github.com/netty/netty/issues/203
    static {
        final String key = "sun.nio.ch.bugLevel";
        final String buglevel = SystemPropertyUtil.get(key);
        if (buglevel == null) {
            try {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        System.setProperty(key, "");
                        return null;
                    }
                });
            } catch (final SecurityException e) {
                logger.debug("Unable to get/set System Property: " + key, e);
            }
        }

        int selectorAutoRebuildThreshold = SystemPropertyUtil.getInt("io.netty.selectorAutoRebuildThreshold", 512);
        if (selectorAutoRebuildThreshold < MIN_PREMATURE_SELECTOR_RETURNS) {
            selectorAutoRebuildThreshold = 0;
        }

        SELECTOR_AUTO_REBUILD_THRESHOLD = selectorAutoRebuildThreshold;

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.noKeySetOptimization: {}", DISABLE_KEYSET_OPTIMIZATION);
            logger.debug("-Dio.netty.selectorAutoRebuildThreshold: {}", SELECTOR_AUTO_REBUILD_THRESHOLD);
        }
    }

    /**
     * The NIO {@link Selector}.
     */
    private Selector selector;
    private Selector unwrappedSelector;
    private SelectedSelectionKeySet selectedKeys;

    private final SelectorProvider provider;

    /**
     * Boolean that controls determines if a blocked Selector.select should
     * break out of its selection process. In our case we use a timeout for
     * the select method and the select method will block for that time unless
     * waken up.
     */
    private final AtomicBoolean wakenUp = new AtomicBoolean();

    private final SelectStrategy selectStrategy;

    private volatile int ioRatio = 50;
    private int cancelledKeys;
    private boolean needsToSelectAgain;

    // todo 这里是 它的构造方法
    NioEventLoop(NioEventLoopGroup parent, Executor executor, SelectorProvider selectorProvider,
                 SelectStrategy strategy, RejectedExecutionHandler rejectedExecutionHandler) {
        // todo 进入到父类,  着重看他是如何创建出 TaskQueue的
        super(parent, executor, false, DEFAULT_MAX_PENDING_TASKS, rejectedExecutionHandler);
        if (selectorProvider == null) {
            throw new NullPointerException("selectorProvider");
        }
        if (strategy == null) {
            throw new NullPointerException("selectStrategy");
        }
        // todo 常用的 属性
        provider = selectorProvider;

        // todo 获取Selector选择器, 可以着重看一下这个 openSelector方法, 看看netty如果打开选择器, 和原生jdk有哪些出入
        final SelectorTuple selectorTuple = openSelector();

        // todo SelectorTuple是netty维护 jdk 原生的Selector的包装类, 下面看,他有两个Selector, 一个是经过包装的,一个是未经过包装的
        selector = selectorTuple.selector;  //
        unwrappedSelector = selectorTuple.unwrappedSelector;  // todo Jdk 原生的Selector
        selectStrategy = strategy;
    }

    private static final class SelectorTuple {
        final Selector unwrappedSelector;
        final Selector selector;

        SelectorTuple(Selector unwrappedSelector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = unwrappedSelector;
        }

        SelectorTuple(Selector unwrappedSelector, Selector selector) {
            this.unwrappedSelector = unwrappedSelector;
            this.selector = selector;
        }
    }

    /**
     * todo 这里进行了优化,netty把hashSet转换成了数组,因为在JDK的NIO模型中,获取Selector时,Selector里面内置的存放SelectionKey的容器是Ｓｅｔ集合
     *
     * todo 说这个优化之前就不得不回想一下原生的JDK的 NIO编程模型中的几大组件, 1. Selector 2. Channel 3. ByteBuffer
     * todo 其中的Selector 中主要维护了三个set集合, 分别是 1. keySet  2. Selectedkey  3.cannelledKey  这三个容器的底层都是set结构
     *
     * todo  而netty把上面的SelectedKey 替换成了自己的的数据接口, 数组, 从而使在任何情况下,它的时间复杂度都是 O1
     * @return
     */
    private SelectorTuple openSelector() {
        final Selector unwrappedSelector;
        try {
            // todo 使用jdk 的api创建新的 selector
            unwrappedSelector = provider.openSelector();
        } catch (IOException e) {
            throw new ChannelException("failed to open a new selector", e);
        }

        if (DISABLE_KEYSET_OPTIMIZATION) { // todo 如果不需要优化,就返回原生的selector , 默认为false 即 使用优化
            return new SelectorTuple(unwrappedSelector);
        }
        // todo  接下来 netty会用下面这个SelectedSelectionKeySet数据结构 替换原来的 keySet , 进入查看
        final SelectedSelectionKeySet selectedKeySet = new SelectedSelectionKeySet();

        Object maybeSelectorImplClass = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    // todo 通过反射  sun.nio.ch.SelectorImpl 或者这个类
                    return Class.forName(
                            "sun.nio.ch.SelectorImpl",
                            false,
                            PlatformDependent.getSystemClassLoader());
                } catch (Throwable cause) {
                    return cause;
                }
            }
        });
        // todo 判断是否获取到了这个类
        if (!(maybeSelectorImplClass instanceof Class) ||
                // ensure the current selector implementation is what we can instrument.
                !((Class<?>) maybeSelectorImplClass).isAssignableFrom(unwrappedSelector.getClass())) {
            if (maybeSelectorImplClass instanceof Throwable) {
                Throwable t = (Throwable) maybeSelectorImplClass;
                logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, t);
            }
            return new SelectorTuple(unwrappedSelector);
        }

        // todo 确定是Selector的实现类  换了个名字
        final Class<?> selectorImplClass = (Class<?>) maybeSelectorImplClass;

        /**
         * todo 类java.security.AccessController提供了一个默认的安全策略执行机制，它使用栈检查来决定潜在不安全的操作是否被允许。
         * todo 这个访问控制器不能被实例化，它不是一个对象，而是集合在单个类中的多个静态方法。
         */
        Object maybeException = AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    // todo 通过反射, 获取到 selectorImplClass的两个字段 selectedKeys   publicSelectedKeys
                    // todo  selectedKeys   publicSelectedKeys底层都是 hashSet() 实现的, 现在获取出来了, 放入上面的数组数据结构中
                    Field selectedKeysField = selectorImplClass.getDeclaredField("selectedKeys");
                    Field publicSelectedKeysField = selectorImplClass.getDeclaredField("publicSelectedKeys");

                    // todo trySetAccessible 可以强制访问私有的对象
                    Throwable cause = ReflectionUtil.trySetAccessible(selectedKeysField);
                    if (cause != null) {
                        return cause;
                    }
                    // todo trySetAccessible 可以强制访问私有的对象
                    cause = ReflectionUtil.trySetAccessible(publicSelectedKeysField);
                    if (cause != null) {
                        return cause;
                    }
                    // todo  真正的把通过反射得到的 那两个字段放入我们自己的数据结构中
                    // todo  下面是把我们的NioEventLoop中的 unwrappedSelector 的 selectedKeysField的属性 直接设置成 优化后的selectedKeySet
                    selectedKeysField.set(unwrappedSelector, selectedKeySet);
                    publicSelectedKeysField.set(unwrappedSelector, selectedKeySet);
                    return null;
                } catch (NoSuchFieldException e) {
                    return e;
                } catch (IllegalAccessException e) {
                    return e;
                }
            }
        });

        if (maybeException instanceof Exception) {
            selectedKeys = null;
            Exception e = (Exception) maybeException;
            logger.trace("failed to instrument a special java.util.Set into: {}", unwrappedSelector, e);
            return new SelectorTuple(unwrappedSelector);
        }

        // todo 初始化自己维护被选中的key的集合  --> 数组类型的
        selectedKeys = selectedKeySet;
        logger.trace("instrumented a special java.util.Set into: {}", unwrappedSelector);
        return new SelectorTuple(unwrappedSelector,
                new SelectedSelectionKeySetSelector(unwrappedSelector, selectedKeySet));
    }

    /**
     * Returns the {@link SelectorProvider} used by this {@link NioEventLoop} to obtain the {@link Selector}.
     */
    public SelectorProvider selectorProvider() {
        return provider;
    }

    /**todo 来这里之前, 我们将线程的执行器交给了 EventLoop管理, 使得EventLoop拥有了执行任务的能力
     * todo 现在, 为每一个EventLoop 创建一个任务队列, MpscQueue, 这个队列的特点就是 使用于Netty现在使用的线程模型, 单消费者,多生产者
     * @param maxPendingTasks
     * @return
     */
    @Override
    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        // This event loop never calls takeTask()
        return maxPendingTasks == Integer.MAX_VALUE ? PlatformDependent.<Runnable>newMpscQueue()
                : PlatformDependent.<Runnable>newMpscQueue(maxPendingTasks);
    }

    @Override
    public int pendingTasks() {
        // As we use a MpscQueue we need to ensure pendingTasks() is only executed from within the EventLoop as
        // otherwise we may see unexpected behavior (as size() is only allowed to be called by a single consumer).
        // See https://github.com/netty/netty/issues/5297
        if (inEventLoop()) {
            return super.pendingTasks();
        } else {
            return submit(pendingTasksCallable).syncUninterruptibly().getNow();
        }
    }

    /**
     * Registers an arbitrary {@link SelectableChannel}, not necessarily created by Netty, to the {@link Selector}
     * of this event loop.  Once the specified {@link SelectableChannel} is registered, the specified {@code task} will
     * be executed by this event loop when the {@link SelectableChannel} is ready.
     */
    public void register(final SelectableChannel ch, final int interestOps, final NioTask<?> task) {
        if (ch == null) {
            throw new NullPointerException("ch");
        }
        if (interestOps == 0) {
            throw new IllegalArgumentException("interestOps must be non-zero.");
        }
        if ((interestOps & ~ch.validOps()) != 0) {
            throw new IllegalArgumentException(
                    "invalid interestOps: " + interestOps + "(validOps: " + ch.validOps() + ')');
        }
        if (task == null) {
            throw new NullPointerException("task");
        }

        if (isShutdown()) {
            throw new IllegalStateException("event loop shut down");
        }

        try {
            ch.register(selector, interestOps, task);
        } catch (Exception e) {
            throw new EventLoopException("failed to register a channel", e);
        }
    }

    /**
     * Returns the percentage of the desired amount of time spent for I/O in the event loop.
     */
    public int getIoRatio() {
        return ioRatio;
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the event loop.  The default value is
     * {@code 50}, which means the event loop will try to spend the same amount of time for I/O as for non-I/O tasks.
     */
    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }

    /**
     * Replaces the current {@link Selector} of this event loop with newly created {@link Selector}s to work
     * around the infamous epoll 100% CPU bug.
     */
    public void rebuildSelector() {
        if (!inEventLoop()) {
            execute(new Runnable() {
                @Override
                public void run() {
                    rebuildSelector0();
                }
            });
            return;
        }
        rebuildSelector0();
    }

    private void rebuildSelector0() {
        final Selector oldSelector = selector;
        final SelectorTuple newSelectorTuple;

        if (oldSelector == null) {
            return;
        }

        try {
            // todo 重新创建一个selector
            newSelectorTuple = openSelector();
        } catch (Exception e) {
            logger.warn("Failed to create a new Selector.", e);
            return;
        }

        // Register all channels to the new Selector.
        int nChannels = 0;
        // todo 循环原来的keys
        for (SelectionKey key : oldSelector.keys()) {
            Object a = key.attachment();
            try {
                if (!key.isValid() || key.channel().keyFor(newSelectorTuple.unwrappedSelector) != null) {
                    continue;
                }

                int interestOps = key.interestOps();
                // todo 把原来的key取消,
                key.cancel();
                // todo 把原来的注册进新的seleor上面
                SelectionKey newKey = key.channel().register(newSelectorTuple.unwrappedSelector, interestOps, a);
                if (a instanceof AbstractNioChannel) {
                    // Update SelectionKey
                    ((AbstractNioChannel) a).selectionKey = newKey;
                }
                nChannels++;
            } catch (Exception e) {
                logger.warn("Failed to re-register a Channel to the new Selector.", e);
                if (a instanceof AbstractNioChannel) {
                    AbstractNioChannel ch = (AbstractNioChannel) a;
                    ch.unsafe().close(ch.unsafe().voidPromise());
                } else {
                    @SuppressWarnings("unchecked")
                    NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                    invokeChannelUnregistered(task, key, e);
                }
            }
        }

        selector = newSelectorTuple.selector;
        unwrappedSelector = newSelectorTuple.unwrappedSelector;

        try {
            // time to close the old selector as everything else is registered to the new one
            oldSelector.close();
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to close the old Selector.", t);
            }
        }

        logger.info("Migrated " + nChannels + " channel(s) to the new Selector.");
    }
    // todo 这里的NioEventLoop的run方法相当于我们的 手动编写的while true循环

    /**
     * todo select()                    检查是否有IO事件
     * todo ProcessorSelectedKeys()     处理IO事件
     * todo RunAllTask()                处理异步任务队列
     */
    @Override
    protected void run() {
        for (; ; ) {
            try {
                switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
                    case SelectStrategy.CONTINUE:
                        continue;
                    case SelectStrategy.SELECT:
                        // todo 轮询IO事件, 等待事件的发生, 本方法下面的代码是处理接受到的感性趣的事件, 进入查看本方法
                        select(wakenUp.getAndSet(false));

                        // wakenUp.compareAndSet(false, true)' is always evaluated
                        // before calling 'selector.wakeup()' to reduce the wake-up
                        // overhead. (Selector.wakeup() is an expensive operation.)
                        //
                        // However, there is a race condition in this approach.
                        // The race condition is triggered when 'wakenUp' is set to
                        // true too early.
                        //
                        // 'wakenUp' is set to true too early if:
                        // 1) Selector is waken up between 'wakenUp.set(false)' and
                        //    'selector.select(...)'. (BAD)
                        // 2) Selector is waken up between 'selector.select(...)' and
                        //    'if (wakenUp.get()) { ... }'. (OK)
                        //
                        // In the first case, 'wakenUp' is set to true and the
                        // following 'selector.select(...)' will wake up immediately.
                        // Until 'wakenUp' is set to false again in the next round,
                        // 'wakenUp.compareAndSet(false, true)' will fail, and therefore
                        // any attempt to wake up the Selector will fail, too, causing
                        // the following 'selector.select(...)' call to block
                        // unnecessarily.
                        //
                        // To fix this problem, we wake up the selector again if wakenUp
                        // is true immediately after selector.select(...).
                        // It is inefficient in that it wakes up the selector for both
                        // the first case (BAD - wake-up required) and the second case
                        // (OK - no wake-up required).

                        if (wakenUp.get()) {
                            selector.wakeup();
                        }
                        // fall through
                    default:
                }

                cancelledKeys = 0;
                needsToSelectAgain = false;
                final int ioRatio = this.ioRatio;  // todo 默认50
                // todo  如果ioRatio==100 就调用第一个     processSelectedKeys();  否则就调用第二个
                if (ioRatio == 100) {
                    try {
                        // todo 处理 处理发生的感性趣的事件
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        // todo 用于处理 本 eventLoop外的线程 扔到taskQueue中的任务
                        runAllTasks();
                    }
                } else {// todo 因为ioRatio默认是50 , 所以来else
                    // todo 记录下开始的时间
                    final long ioStartTime = System.nanoTime();
                    try {
                        // todo 处理IO事件
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        // todo  根据处理IO事件耗时 ,控制 下面的runAllTasks执行任务不能超过 ioTime 时间
                        final long ioTime = System.nanoTime() - ioStartTime;
                        // todo 这里面有聚合任务的逻辑
                        runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
            // Always handle shutdown even if the loop processing threw an exception.
            try {
                if (isShuttingDown()) {
                    closeAll();
                    if (confirmShutdown()) {
                        return;
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
        }
    }

    private static void handleLoopException(Throwable t) {
        logger.warn("Unexpected exception in the selector loop.", t);

        // Prevent possible consecutive immediate failures that lead to
        // excessive CPU consumption.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore.
        }
    }

    private void processSelectedKeys() {
            // todo  selectedKeys 就是经过优化后的keys(底层是数组) , 默认不为null
            if (selectedKeys != null) {
                processSelectedKeysOptimized();
            } else {
                processSelectedKeysPlain(selector.selectedKeys());
            }
    }

    @Override
    protected void cleanup() {
        try {
            selector.close();
        } catch (IOException e) {
            logger.warn("Failed to close a selector.", e);
        }
    }

    void cancel(SelectionKey key) {
        key.cancel();
        cancelledKeys++;
        if (cancelledKeys >= CLEANUP_INTERVAL) {
            cancelledKeys = 0;
            needsToSelectAgain = true;
        }
    }

    @Override
    protected Runnable pollTask() {
        Runnable task = super.pollTask();
        if (needsToSelectAgain) {
            selectAgain();
        }
        return task;
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }

        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (; ; ) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();

            if (a instanceof AbstractNioChannel) {
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (!i.hasNext()) {
                break;
            }

            if (needsToSelectAgain) {
                selectAgain();
                selectedKeys = selector.selectedKeys();

                // Create the iterator again to avoid ConcurrentModificationException
                if (selectedKeys.isEmpty()) {
                    break;
                } else {
                    i = selectedKeys.iterator();
                }
            }
        }
    }

    private void processSelectedKeysOptimized() {
        for (int i = 0; i < selectedKeys.size; ++i) {
            final SelectionKey k = selectedKeys.keys[i];
            // null out entry in the array to allow to have it GC'ed once the Channel close
            // todo 数组输出空项, 从而允许在channel 关闭时对其进行垃圾回收
            // See https://github.com/netty/netty/issues/2363
            // todo 数组中当前循环对应的keys质空, 这种感兴趣的事件只处理一次就行
            selectedKeys.keys[i] = null;

            // todo 获取出 attachment,默认情况下就是注册进Selector时,传入的第三个参数  this===>   NioServerSocketChannel
            // todo 一个Selector中可能被绑定上了成千上万个Channel,  通过K+attachment 的手段, 精确的取出发生指定事件的channel,
            // todo 进而获取channel中的unsafe类进行下一步处理
            final Object a = k.attachment();
            // todo

            if (a instanceof AbstractNioChannel) {
                // todo 进入这个方法, 传进入 感兴趣的key + NioSocketChannel
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (needsToSelectAgain) {
                // null out entries in the array to allow to have it GC'ed once the Channel close
                // See https://github.com/netty/netty/issues/2363
                selectedKeys.reset(i + 1);

                selectAgain();
                i = -1;
            }
        }
    }

    // todo 服务端启动后,方法被用用处理新链接,  可以模拟 telnet localhost 8899 新链接的介入
    // todo 处理selectedkey
    // todo netty底层对数据的读写都是  unsafe完成的
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        // todo 这个unsafe 也是可channel 也是和Channel进行唯一绑定的对象
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        if (!k.isValid()) {   // todo 确保Key的合法
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                // If the channel implementation throws an exception because there is no event loop, we ignore this
                // because we are only trying to determine if ch is registered to this event loop and thus has authority
                // to close ch.
                return;
            }
            // Only close ch if ch is still registered to this EventLoop. ch could have deregistered from the event loop
            // and thus the SelectionKey could be cancelled as part of the deregistration process, but the channel is
            // still healthy and should not be closed.
            // See https://github.com/netty/netty/issues/5125
            if (eventLoop != this || eventLoop == null) { // todo 确保多线程下的安全性
                return;
            }
            // close the channel if the key is not valid anymore
            unsafe.close(unsafe.voidPromise());
            return;
        }
        // todo NioServerSocketChannel和selectKey都合法的话, 就进入下面的 处理阶段
        try {
            // todo 获取SelectedKey 的 关心的选项
            int readyOps = k.readyOps();
            // We first need to call finishConnect() before try to trigger a read(...) or write(...) as otherwise
            // the NIO JDK channel implementation may throw a NotYetConnectedException.
            // todo 在read()   write()之前我们需要调用 finishConnect()  方法, 否则  NIO JDK抛出异常
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
                // See https://github.com/netty/netty/issues/924
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps( );

                unsafe.finishConnect();
            }

            // Process OP_WRITE first as we may be able to write some queued buffers and so free memory.
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to  write
                ch.unsafe().forceFlush();
            }

            // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
            // to a spin loop
            // todo 同样是检查 readOps是否为零, 来检查是否出现了  jdk  空轮询的bug
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }

    // todo  断点调试
    private static void processSelectedKey(SelectionKey k, NioTask<SelectableChannel> task) {
        int state = 0;
        try {
            task.channelReady(k.channel(), k);
            state = 1;
        } catch (Exception e) {
            k.cancel();
            invokeChannelUnregistered(task, k, e);
            state = 2;
        } finally {
            switch (state) {
                case 0:
                    k.cancel();
                    invokeChannelUnregistered(task, k, null);
                    break;
                case 1:
                    if (!k.isValid()) { // Cancelled by channelReady()
                        invokeChannelUnregistered(task, k, null);
                    }
                    break;
            }
        }
    }

    private void closeAll() {
        selectAgain();
        Set<SelectionKey> keys = selector.keys();
        Collection<AbstractNioChannel> channels = new ArrayList<AbstractNioChannel>(keys.size());
        for (SelectionKey k : keys) {
            Object a = k.attachment();
            if (a instanceof AbstractNioChannel) {
                channels.add((AbstractNioChannel) a);
            } else {
                k.cancel();
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                invokeChannelUnregistered(task, k, null);
            }
        }

        for (AbstractNioChannel ch : channels) {
            ch.unsafe().close(ch.unsafe().voidPromise());
        }
    }

    private static void invokeChannelUnregistered(NioTask<SelectableChannel> task, SelectionKey k, Throwable cause) {
        try {
            task.channelUnregistered(k.channel(), cause);
        } catch (Exception e) {
            logger.warn("Unexpected exception while running NioTask.channelUnregistered()", e);
        }
    }

    @Override
    protected void wakeup(boolean inEventLoop) {
        if (!inEventLoop && wakenUp.compareAndSet(false, true)) {
            selector.wakeup();
        }
    }

    Selector unwrappedSelector() {
        return unwrappedSelector;
    }

    int selectNow() throws IOException {
        try {
            return selector.selectNow();
        } finally {
            // restore wakeup state if needed
            if (wakenUp.get()) {
                selector.wakeup();
            }
        }
    }

    // todo 循环接受IO事件
    // todo 每次进行 select()  操作时, oldWakenUp被标记为false
    private void select(boolean oldWakenUp) throws IOException {
        Selector selector = this.selector;
        try {
            ///todo ----------------------------------------- 如下部分代码, 是 select()的deadLine及任务穿插处理逻辑-----------------------------------------------------
           // todo selectCnt这个变量记录了 循环 select的次数
            int selectCnt = 0;
            // todo 记录当前时间
            long currentTimeNanos = System.nanoTime();
            // todo 计算出估算的截止时间,  意思是, select()操作不能超过selectDeadLineNanos这个时间, 不让它一直耗着,外面也可能有任务等着当前线程处理
            long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);

                //todo -------for 循环开始  -------
            for (; ;) {
                // todo 计算超时时间
                long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;
                if (timeoutMillis <= 0) {// todo 如果超时了 , 并且selectCnt==0 , 就进行非阻塞的 select() , break, 跳出for循环
                    if (selectCnt == 0) {
                        selector.selectNow();
                        selectCnt = 1;
                    }
                    break;
                }

                // If a task was submitted when wakenUp value was true, the task didn't get a chance to call
                // Selector#wakeup. So we need to check task queue again before executing select operation.
                // If we don't, the task might be pended until select operation was timed out.
                // It might be pended until idle timeout if IdleStateHandler existed in pipeline.
                // todo  判断任务队列中时候还有别的任务, 如果有任务的话, 进入代码块, 非阻塞的select() 并且 break; 跳出循环
                //todo  通过cas 把线程安全的把 wakenU设置成true表示退出select()方法, 已进入时,我们设置oldWakenUp是false
                if (hasTasks() && wakenUp.compareAndSet(false, true)) {
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }
                ///todo ----------------------------------------- 如上部分代码, 是 select()的deadLine及任务穿插处理逻辑-----------------------------------------------------


                ///todo ----------------------------------------- 如下, 是 阻塞式的select() -----------------------------------------------------

                // todo  上面设置的超时时间没到,而且任务为空,进行阻塞式的 select() , timeoutMillis 默认1
                // todo netty任务,现在可以放心大胆的 阻塞1秒去轮询 channel连接上是否发生的 selector感性的事件
                int selectedKeys = selector.select(timeoutMillis);

                // todo 表示当前已经轮询了SelectCnt次了
                selectCnt++;

                // todo 阻塞完成轮询后,马上进一步判断 只要满足下面的任意一条. 也将退出无限for循环, select()
                // todo  selectedKeys != 0       表示轮询到了事件
                // todo  oldWakenUp              当前的操作是否需要唤醒
                // todo  wakenUp.get()           可能被外部线程唤醒
                // todo  hasTasks()              任务队列中又有新任务了
                // todo  hasScheduledTasks()     当时定时任务队列里面也有任务
                if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || hasTasks() || hasScheduledTasks()) {
                    // - Selected something,
                    // - waken up by user, or
                    // - the task queue has a pending task.
                    // - a scheduled task is ready for processing
                    break;
                }
                ///todo ----------------------------------------- 如上, 是 阻塞式的select() -----------------------------------------------------


                if (Thread.interrupted()) {
                    // Thread was interrupted so reset selected keys and break so we not run into a busy loop.
                    // As this is most likely a bug in the handler of the user or it's client library we will
                    // also log it.
                    //
                    // See https://github.com/netty/netty/issues/2426
                    if (logger.isDebugEnabled()) {
                        logger.debug("Selector.select() returned prematurely because " +
                                "Thread.currentThread().interrupt() was called. Use " +
                                "NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
                    }
                    selectCnt = 1;
                    break;
                }
                // todo 每次执行到这里就说明,已经进行了一次阻塞式操作 ,并且还没有监听到任何感兴趣的事件,也没有新的任务添加到队列,  记录当前的时间
                long time = System.nanoTime();

                // todo  每次for循环都会判断, 当前时间 currentTimeNanos 不能超过预订的超时时间 timeoutMillis
                // todo  但是,现在的情况是, 虽然已经进行了一次 时长为timeoutMillis时间的阻塞式select了,
                // todo  然而, 我执行到当前代码的 时间 - 开始的时间 >= 超时的时间

                // todo 但是   如果 当前时间- 超时时间< 开始时间, 也就是说,并没有阻塞select, 而是立即返回了, 就表明这是一次空轮询
                // todo 而每次轮询   selectCnt ++;  于是有了下面的判断,
                if (time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos) {
                    // timeoutMillis elapsed without anything selected.
                    selectCnt = 1;
                } else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                        // todo  selectCnt如果大于 512 表示cpu确实在空轮询, 于是rebuild Selector
                        selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                    // The selector returned prematurely many times in a row.
                    // Rebuild the selector to work around the problem.
                    logger.warn(
                            "Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                            selectCnt, selector);
                    // todo 它的逻辑创建一个新的selectKey , 把老的Selector上面的key注册进这个新的selector上面 , 进入查看
                    rebuildSelector();
                    selector = this.selector;

                    // Select again to populate selectedKeys.
                    // todo 解决了Select空轮询的bug
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }

                currentTimeNanos = time;
            }

            ////   -----------for 循环结束 --------------


            if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Selector.select() returned prematurely {} times in a row for Selector {}.",
                            selectCnt - 1, selector);
                }
            }
        } catch (CancelledKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                        selector, e);
            }
            // Harmless exception - log anyway
        }
    }

    private void selectAgain() {
        needsToSelectAgain = false;
        try {
            selector.selectNow();
        } catch (Throwable t) {
            logger.warn("Failed to update SelectionKeys.", t);
        }
    }
}
