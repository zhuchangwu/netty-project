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

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Special {@link EventExecutorGroup} which allows registering {@link Channel}s that get
 * processed for later selection during the event loop.
 * todo 他是个特殊的 EventExecutorGroup, 允许在选择器进行死循环等待事件发生的时候, 依然允许channel(跟客户端的连接)注册进来
 */
public interface EventLoopGroup extends EventExecutorGroup {
    /**
     * Return the next {@link EventLoop} to use
     * // todo 返回下一个要使用的事件循环, 下一个 EventLoop,
     * // todo 目的是将客户端的channel 平均的添加进所有的 EventLoop 中
     */
    @Override
    EventLoop next();

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The returned {@link ChannelFuture}
     * will get notified once the registration was complete.
     * todo 将channel注册进EventLoop中 , 返回一个ChannelFuture, 这是个异步的方法, 一旦调用马上就返回一个 未来连接对象 (1.5里的future),
     * todo 我们通过ChannelFutrue channel是否成功注册进了 EventLoop中
     */
    ChannelFuture register(Channel channel);

    /**
     * Register a {@link Channel} with this {@link EventLoop} using a {@link ChannelFuture}. The passed
     * {@link ChannelFuture} will get notified once the registration was complete and also will get returned.
     */
    // todo 这个 ChannelPromise 继承了 ChannelFuture ,ChannelPromise中有channel的引用, 目的还是将channel 注册进Eventloop中
    // todo 同样是将ChannelPromise里面所包含的 channel注册进事件循环中
    ChannelFuture register(ChannelPromise promise);

    /**
     * Register a {@link Channel} with this {@link EventLoop}. The passed {@link ChannelFuture}
     * will get notified once the registration was complete and also will get returned.
     *
     * @deprecated Use {@link #register(ChannelPromise)} instead.
     */
    @Deprecated
    ChannelFuture register(Channel channel, ChannelPromise promise);
}
