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
package io.netty.util.concurrent;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for {@link EventExecutorGroup} implementations that handles their tasks with multiple threads at
 * * the same time.
 * todo  他是EventExecutorGroup接口 下 的 抽象基类, 它实现了使用多个线程处理任务
 * todo  多线程事件执行器组,
 */
public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {

    // todo 事件执行器的数组, 里面是一个一个的executor 事件执行器;  事件循环组中有多少个事件循环,这里就有多少个事件循环组
    private final EventExecutor[] children;
    private final Set<EventExecutor> readonlyChildren;
    private final AtomicInteger terminatedChildren = new AtomicInteger();
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    //todo chooser == EventExecutorChooser 他是一个用于做选择的组件
    private final EventExecutorChooserFactory.EventExecutorChooser chooser;

    /**
     * Create a new instance.
     *
     * @param nThreads      the number of threads that will be used by this instance.
     * @param threadFactory the ThreadFactory to use, or {@code null} if the default should be used.
     * @param args          arguments which will passed to each {@link #newChild(Executor, Object...)} call
     */
    protected MultithreadEventExecutorGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        this(nThreads, threadFactory == null ? null : new ThreadPerTaskExecutor(threadFactory), args);
    }

    /**
     * Create a new instance.
     *
     * @param nThreads the number of threads that will be used by this instance.
     * @param executor the Executor to use, or {@code null} if the default should be used.
     * @param args     arguments which will passed to each {@link #newChild(Executor, Object...)} call
     */
    // todo 调用 MultithreadEventExecutorGroup 多线程事件执行组的构造方法
    // todo  Object... args  是 selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject()的简写
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor, Object... args) {
        this(nThreads, executor, DefaultEventExecutorChooserFactory.INSTANCE, args);
    }

    /**
     * Create a new instance.
     *
     * @param nThreads       todo 当前这个线程组所拥有的线程数    在我的机器上默认是16条    the number of threads that will be used by this instance.
     * @param executor       the Executor to use, or {@code null} if the default should be used.
     * @param chooserFactory the {@link EventExecutorChooserFactory} to use.
     * @param args           arguments which will passed to each {@link #newChild(Executor, Object...)} call
     */

    // todo 在这个构造方法中,完成了一些属性的赋值, 彻底构造完成  事件循环组对象
    // todo  Object... args  是 selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject()的简写
    protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
                                            EventExecutorChooserFactory chooserFactory, Object... args) {
        if (nThreads <= 0) {
            throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
        }

        if (executor == null) {
            // todo 下面需要的参数,一开始使用无参的构造方法时, 传递进来的 就是null ,执行这一行代码, 创建默认的线程工厂
            // todo ThreadPerTaskExecutor  意味为当前的事件循环组   创建Executor , 用于 针对每一个任务的Executor   线程的执行器
            // todo  newDefaultThreadFactory根据它的特性,可以给线程加名字等,
            // todo 比传统的好处是 把创建线程和 定义线程需要做的任务分开, 我们只关心任务,  两者解耦
            // todo 每次执行任务都会创建一个线程实体
            // todo NioEventLoop 线程命名规则  nioEventLoop-1-XX    1代表是第几个group   XX第几个eventLoop
            executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
        }
        // todo children是线程执行器组里面的线程执行器
        children = new EventExecutor[nThreads];

        // todo  循环实例化
        for (int i = 0; i < nThreads; i++) {
            boolean success = false;
            try {
                // todo 创建EventLoop, 直接看一下这个newChild , 它的返回值是 EventExecutor , 但是如果我们直接去看它子类的实现的话
                // todo 会发现new 的实例其实的 NioEventLoop, 这并不奇怪, 因为,NioEventLoop间接实现了 EventExecutor 接口
                // todo 换句话说, 其实就是 NioEventLoop 就是 拥有线程执行器的功能
                children[i] = newChild(executor, args);
                success = true;
            } catch (Exception e) {
                // TODO: Think about if this is a good exception type
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                if (!success) {
                    for (int j = 0; j < i; j++) {
                        children[j].shutdownGracefully();
                    }

                    for (int j = 0; j < i; j++) {
                        EventExecutor e = children[j];
                        try {
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            // Let the caller handle the interruption.
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }
        // todo chooser 在这里 初始化了
        chooser = chooserFactory.newChooser(children);

        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                if (terminatedChildren.incrementAndGet() == children.length) {
                    terminationFuture.setSuccess(null);
                }
            }
        };

        for (EventExecutor e : children) {
            e.terminationFuture().addListener(terminationListener);
        }

        Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);
        Collections.addAll(childrenSet, children);
        readonlyChildren = Collections.unmodifiableSet(childrenSet);
    }

    // todo 创建默认的线程工厂
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass());
    }

    @Override
    public EventExecutor next() {
        // todo  chooser - > EventExecutorChooser 跟进去
        return chooser.next();
    }

    @Override
    public Iterator<EventExecutor> iterator() {
        return readonlyChildren.iterator();
    }

    /**
     * Return the number of {@link EventExecutor} this implementation uses. This number is the maps
     * 1:1 to the threads it use.
     */
    public final int executorCount() {
        return children.length;
    }

    // todo ,创建一个 事件执行器,一会可以通过next方法使用 ,具体实现是我们在启动demo中 new的NioEventLoopGroup
    protected abstract EventExecutor newChild(Executor executor, Object... args) throws Exception;

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        for (EventExecutor l : children) {
            l.shutdownGracefully(quietPeriod, timeout, unit);
        }
        return terminationFuture();
    }

    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    @Deprecated
    public void shutdown() {
        for (EventExecutor l : children) {
            l.shutdown();
        }
    }

    @Override
    public boolean isShuttingDown() {
        for (EventExecutor l : children) {
            if (!l.isShuttingDown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isShutdown() {
        for (EventExecutor l : children) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        for (EventExecutor l : children) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        loop:
        for (EventExecutor l : children) {
            for (; ; ) {
                long timeLeft = deadline - System.nanoTime();
                if (timeLeft <= 0) {
                    break loop;
                }
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }
        return isTerminated();
    }
}
