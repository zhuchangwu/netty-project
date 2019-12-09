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
package io.netty.channel;

import io.netty.util.NettyRuntime;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * Abstract base class for {@link EventLoopGroup} implementations that handles their tasks with multiple threads at
 * the same time.
 */
public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);

    private static final int DEFAULT_EVENT_LOOP_THREADS;

    // todo 我们new NioEventLoopGroup, 在创建NioEventLoopGroup之前会先来到它的父类执行父类的静态方法
    // todo 当MultithreadEventLoopGroup被加载进 JVM就会执行, 对 DEFAULT_EVENT_LOOP_THREADS进行初始化
    static {
        // todo   max方法取最大值,
        // todo  SystemPropertyUtil.getInt,这是个系统辅助类, 如果系统中有 io.netty.eventLoopThreads对应的值,就取它, 没有的话,取后面的值使用
        // todo  NettyRuntime.availableProcessors() 是当前的 系统的核数*2  , 在我的电脑四个核心,通过超线程技术就被模拟成八个核心,8*2=16条线程
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }

    /**
     * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int, Executor, Object...)
     */
    // todo  接着 使用父类的构造方法,   nThreads=  DEFAULT_EVENT_LOOP_THREADS
    // todo Object... args  是 selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject()的简写
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }

    /**
     * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int, ThreadFactory, Object...)
     */
    protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
    }

    /**
     * @see MultithreadEventExecutorGroup#MultithreadEventExecutorGroup(int, Executor,
     * EventExecutorChooserFactory, Object...)
     */
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
                                     Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, chooserFactory, args);
    }

    @Override
    protected ThreadFactory newDefaultThreadFactory() {
        return new DefaultThreadFactory(getClass(), Thread.MAX_PRIORITY);
    }

    @Override
    public EventLoop next() {
        // todo 跟进去
        return (EventLoop) super.next();
    }

    @Override
    protected abstract EventLoop newChild(Executor executor, Object... args) throws Exception;


    // todo 当前是在  MultithreadEventLoopGroup  多线程的事件循环组
    // todo 注册, 将channel注册进 事件循环组
    @Override
    public ChannelFuture register(Channel channel) {
        // todo  next()  -- 就在上面->  根据轮询算法获取一个事件的执行器  EventExecutor
        // todo, 而每一个EventLoop对应一个EventExecutor   这里之所以是个组, 是因为, 我的机器内核决定我的  事件循环组有八个线程,
        //  todo ?? ????
        // todo 但是一会的责任并没有一直循环, 难道有效的bossGroup只有一条

        // todo 再进去就是SingleThreadEventLoop对此方法的实现了
        return next().register(channel);
    }

    @Override
    public ChannelFuture register(ChannelPromise promise) {
        return next().register(promise);
    }

    @Deprecated
    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return next().register(channel, promise);
    }
}
