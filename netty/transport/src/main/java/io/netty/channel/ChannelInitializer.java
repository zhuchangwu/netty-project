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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentMap;

/**
 * A special {@link ChannelInboundHandler} which offers an easy way to initialize a {@link Channel} once it was
 * registered to its {@link EventLoop}.
 * // todo 他是一个特殊的 ChannelInboundHandler (特殊的channel入栈处理器) , 它提供了 一种 简单的方法初始化channel
 *
 * Implementations are most often used in the context of {@link Bootstrap#handler(ChannelHandler)} ,
 * {@link ServerBootstrap#handler(ChannelHandler)} and {@link ServerBootstrap#childHandler(ChannelHandler)} to
 * setup the {@link ChannelPipeline} of a {@link Channel}.
 * // todo 她会在上面那几种情况下   给Channel 初始化好 管道
 *  * <pre>
 *
 * public class MyChannelInitializer extends {@link ChannelInitializer} {
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("myHandler", new MyHandler());
 *     }
 * }
 *
 * {@link ServerBootstrap} bootstrap = ...;
 * ...
 * bootstrap.childHandler(new MyChannelInitializer());
 * ...
 * </pre>
 * Be aware that this class is marked as {@link Sharable} and so the implementation must be safe to be re-used.
 *
 * @param <C>   A sub-type of {@link Channel}
 */
// todo 他是个 辅助我们编程的类, 可以看到, 一般我们在 BootStrap中添加我们自己实现的继承它的类,然后使用initChannel 根据它的入参,往 pipeline里面一次性添加多个channel
// todo 可以看到,它继承 InBound   表示入栈的 hander适配器
@Sharable  // todo 这个注解是提示用户保证并发情况下的安全性
public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ChannelInitializer.class);
    // We use a ConcurrentMap as a ChannelInitializer is usually shared between all Channels in a Bootstrap /
    // ServerBootstrap. This way we can reduce the memory usage compared to use Attributes.
    private final ConcurrentMap<ChannelHandlerContext, Boolean> initMap = PlatformDependent.newConcurrentHashMap();

    /**
     * This method will be called once the {@link Channel} was registered.
     * todo Channel注册进来时( 注册进EventLoopGroup中的eventLoop中), 方法被调用
     * After the method returns this instance will be removed from the {@link ChannelPipeline} of the {@link Channel}.
     * todo  initChannel(C ch)  方法返回时, instance会从ChannelPipeline 移除
     * todo 为什么会移除 ?　　　因为它的 它虽然是ChannelInboundHandler类型的,
     * todo 但是! 它自己本质上不是handler, 而是一个中介,我们通过他往pipeline 中添加 一个一个的handler  所以被移出!
     * @param ch            the {@link Channel} which was registered.
     * @throws Exception    is thrown if an error occurs. In that case it will be handled by
     *                      {@link #exceptionCaught(ChannelHandlerContext, Throwable)} which will by default close
     *                      the {@link Channel}.
     */
    protected abstract void initChannel(C ch) throws Exception;

    @Override
    @SuppressWarnings("unchecked")
    public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Normally this method will never be called as handlerAdded(...) should call initChannel(...) and remove
        // the handler.
        if (initChannel(ctx)) {
            // we called initChannel(...) so we need to call now pipeline.fireChannelRegistered() to ensure we not
            // miss an event.
            ctx.pipeline().fireChannelRegistered();
        } else {
            // Called initChannel(...) before which is the expected behavior, so just forward the event.
            ctx.fireChannelRegistered();
        }
    }

    /**
     * Handle the {@link Throwable} by logging and closing the {@link Channel}. Sub-classes may override this.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Failed to initialize a channel. Closing: " + ctx.channel(), cause);
        ctx.close();
    }

    @SuppressWarnings("unchecked")
    private boolean initChannel(ChannelHandlerContext ctx) throws Exception {
        if (initMap.putIfAbsent(ctx, Boolean.TRUE) == null) { // Guard against re-entrance.
            try {
                initChannel((C) ctx.channel());
            } catch (Throwable cause) {
                // Explicitly call exceptionCaught(...) as we removed the handler before calling initChannel(...).
                // We do so to prevent multiple calls to initChannel(...).
                exceptionCaught(ctx, cause);
            } finally {
                // todo    remove(ctx);  删除 ChannelInitializer
                remove(ctx);
            }
            return true;
        }
        return false;
    }

    // todo 删除当前ctx 节点
    private void remove(ChannelHandlerContext ctx) {
        try {
            ChannelPipeline pipeline = ctx.pipeline();
            if (pipeline.context(this) != null) {
                pipeline.remove(this);
            }
        } finally {
            initMap.remove(ctx);
        }
    }

    /**
     * {@inheritDoc} If override this method ensure you call super!
     */
    // todo 请确保你在你的父类的spuer中,调用了 下面这个函数
    // todo 这就是移除 Initializer的逻辑 , 而对于服务端来说, 他是在 DefaultChannelPipeline中的 callHandlerAdded0()方法中调用的

    // todo 一旦handlerAdded方法被调用, 进入initChannel()方法,
        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            if (ctx.channel().isRegistered()) {
                // This should always be true with our current DefaultChannelPipeline implementation.
                // The good thing about calling initChannel(...) in handlerAdded(...) is that there will be no ordering
                // surprises if a ChannelInitializer will add another ChannelInitializer. This is as all handlers
                // will be added in the expected order.
                initChannel(ctx); // todo 这个方法在上面, 进入 可以在 finally中 找到移除Initializer的逻辑
            }
    }
}
