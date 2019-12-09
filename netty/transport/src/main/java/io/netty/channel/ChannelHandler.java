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

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Handles an I/O event or intercepts an I/O operation, and forwards it to its next handler in
 * its {@link ChannelPipeline}.
 * todo 处理pipeline中的io操作或者是  并依次向下传递
 * <h3>Sub-types</h3>
 * <p>
 * {@link ChannelHandler} itself does not provide many methods, but you usually have to implement one of its subtypes:
 * <ul>
 * <li>{@link ChannelInboundHandler} to handle inbound I/O events, and</li>
 * <li>{@link ChannelOutboundHandler} to handle outbound I/O operations.</li>
 * </ul> todo 他自己通常不会提供太多的方法,但是你可以 实现 in out bound这两个类去处理出栈,入栈的操作
 * </p>
 * <p>
 * Alternatively, the following adapter classes are provided for your convenience:
 * <ul>
 * <li>{@link ChannelInboundHandlerAdapter} to handle inbound I/O events,</li>
 * <li>{@link ChannelOutboundHandlerAdapter} to handle outbound I/O operations, and</li>
 * <li>{@link ChannelDuplexHandler} to handle both inbound and outbound events</li>
 * </ul>  todo 为了图方便可以选择上面提供好的适配器, 去处理 in out 的 IO操作
 * </p>
 * <p>
 * For more information, please refer to the documentation of each subtype.
 * </p>
 *
 * <h3>The context object</h3>
 * <p>
 * A {@link ChannelHandler} is provided with a {@link ChannelHandlerContext}
 * object.  A {@link ChannelHandler} is supposed to interact with the
 * {@link ChannelPipeline} it belongs to via a context object.  Using the
 * context object, the {@link ChannelHandler} can pass events upstream or
 * downstream, modify the pipeline dynamically, or store the information
 * (using {@link AttributeKey}s) which is specific to the handler.
 * todo 使用 handler我们可以做到, 向上游传递事件,或者向下游传递事件,或者动态的修改管道, 储存信息,用于处理特定的事件的程序
 * <h3>State management</h3>
 *
 * A {@link ChannelHandler} often needs to store some stateful information.
 * The simplest and recommended approach is to use member variables:
 * <pre>
 *     todo 下面是一个例子, handler通常用于存储一些有状态的信息
 * public interface Message {
 *     // your methods here
 * }
 *
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *
 *     <b>private boolean loggedIn;</b>
 *
 *     {@code @Override}
 *     public void channelRead0({@link ChannelHandlerContext} ctx, Message message) {
 *         {@link Channel} ch = e.getChannel();
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) message);
 *             <b>loggedIn = true;</b>
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>loggedIn</b>) {
 *                 ch.write(fetchSecret((GetDataMessage) message));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * Because the handler instance has a state variable which is dedicated to
 * one connection,
 * todo 因为handler实例中存储了一个 专注于建立连接的有状态的变量
 * you have to create a new handler instance for each new
 * channel to avoid a race condition where a unauthenticated client can get
 * the confidential information:
 * todo 所以你必须为每一个新的channel 创建一个新的handler, 用于处理未授权的client去获取机密的信息
 * <pre>
 * // Create a new handler instance per channel.
 * // See {@link ChannelInitializer#initChannel(Channel)}.
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>new DataServerHandler()</b>);
 *     }
 * }
 *
 * </pre>
 * todo 使用 AttributeKey
 * <h4>Using {@link AttributeKey}s</h4>
 *
 * Although it's recommended to use member variables to store the state of a
 * handler, for some reason you might not want to create many handler instances.
 * In such a case, you can use {@link AttributeKey}s which is provided by
 * {@link ChannelHandlerContext}:
 * <pre>
 *     todo 尽管推荐使用一系列的存储有状态的数据的handler, 但是鉴于其他原因, 如果你不想使用instances实例,
 *     todo 在这种情况下, 你可以使用 AttributeKey , 它被 HandlerContext提供
 * public interface Message {
 *     // your methods here
 * }
 *
 * {@code @Sharable}
 * public class DataServerHandler extends {@link SimpleChannelInboundHandler}&lt;Message&gt; {
 *     private final {@link AttributeKey}&lt;{@link Boolean}&gt; auth =
 *           {@link AttributeKey#valueOf(String) AttributeKey.valueOf("auth")};
 *
 *     {@code @Override}
 *     public void channelRead({@link ChannelHandlerContext} ctx, Message message) {
 *         {@link Attribute}&lt;{@link Boolean}&gt; attr = ctx.attr(auth);
 *         {@link Channel} ch = ctx.channel();
 *         if (message instanceof LoginMessage) {
 *             authenticate((LoginMessage) o);
 *             <b>attr.set(true)</b>;
 *         } else (message instanceof GetDataMessage) {
 *             if (<b>Boolean.TRUE.equals(attr.get())</b>) {
 *                 ch.write(fetchSecret((GetDataMessage) o));
 *             } else {
 *                 fail();
 *             }
 *         }
 *     }
 *     ...
 * }
 * </pre>
 * Now that the state of the handler is attached to the {@link ChannelHandlerContext}, you can add the
 * same handler instance to different pipelines:
 * <pre>
 * public class DataServerInitializer extends {@link ChannelInitializer}&lt;{@link Channel}&gt; {
 *
 *     private static final DataServerHandler <b>SHARED</b> = new DataServerHandler();
 *
 *     {@code @Override}
 *     public void initChannel({@link Channel} channel) {
 *         channel.pipeline().addLast("handler", <b>SHARED</b>);
 *     }
 * }
 * </pre>
 *
 *
 * <h4>The {@code @Sharable} annotation</h4>
 * <p>
 * In the example above which used an {@link AttributeKey},
 * you might have noticed the {@code @Sharable} annotation.
 * <p>
 * If a {@link ChannelHandler} is annotated with the {@code @Sharable}
 * annotation, it means you can create an instance of the handler just once and
 * add it to one or more {@link ChannelPipeline}s multiple times without
 * a race condition.
 * <p>  todo @Sharable 意味着你可以把同一个handler 添加在不同的pipeline, 否则不允许
 * If this annotation is not specified, you have to create a new handler
 * instance every time you add it to a pipeline because it has unshared state
 * such as member variables.
 * <p>
 * This annotation is provided for documentation purpose, just like
 * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
 *
 * <h3>Additional resources worth reading</h3>
 * <p>
 * Please refer to the {@link ChannelHandler}, and
 * {@link ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 */
public interface ChannelHandler {

    /**
     * Gets called after the {@link ChannelHandler} was added to the actual context and it's ready to handle events.
     */
    // todo 当handler被添加到真实的上下文中,并且准备处理事件时被调用
    // todo handler 被添加进去的回调
    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called after the {@link ChannelHandler} was removed from the actual context and it doesn't handle events
     * anymore.
     *  todo 是 handler 被移出的后的 回调
     */
    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

    /**
     * Gets called if a {@link Throwable} was thrown.
     *
     * @deprecated is part of {@link ChannelInboundHandler}
     */
    @Deprecated
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    /**
     * Indicates that the same instance of the annotated {@link ChannelHandler}
     * can be added to one or more {@link ChannelPipeline}s multiple times
     * without a race condition.
     * <p>
     * If this annotation is not specified, you have to create a new handler
     * instance every time you add it to a pipeline because it has unshared
     * state such as member variables.
     * <p>
     * This annotation is provided for documentation purpose, just like
     * <a href="http://www.javaconcurrencyinpractice.com/annotations/doc/">the JCIP annotations</a>.
     *  todo 标记这个channelhandler可以 被多个pipeline添加
     */
    @Inherited
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Sharable {
        // no value
    }
}
