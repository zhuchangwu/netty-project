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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import io.netty.util.concurrent.EventExecutor;

import java.nio.channels.Channels;

/**
 * Enables a {@link ChannelHandler} to interact(交互) with its {@link ChannelPipeline}
 * and other handlers.
 * // todo 可以让ChannelHandler 和 ChannelPipeline  以及其他的 handler 进行交互
 *
 * Among other things a handler can notify the next {@link ChannelHandler} in the
 * {@link ChannelPipeline} as well as modify the {@link ChannelPipeline} it belongs to dynamically.
 * // todo  一个handler可以通知ChannelPipline中的下一个handler 或者是动态的修改它所属的 channelPipeline对象
 *
 *
 * <h3>Notify</h3>  // todo 唤醒
 *
 * You can notify the closest handler in the same {@link ChannelPipeline} by calling one of the various methods
 * provided here.
 * // todo 在一个channelPipeline中,你可以唤醒离他最近一个handler 通过 ChannelPipeline 提供的方法
 *
 * Please refer to {@link ChannelPipeline} to understand how an event flows.
 *
 * <h3>Modifying a pipeline</h3>
 *
 * You can get the {@link ChannelPipeline} your handler belongs to by calling
 * {@link #pipeline()}.
 * // todo 你可以调用  pipeline()方法,这个handler所属的 pipeline对象
 *
 * A non-trivial application could insert, remove, or
 * replace handlers in the pipeline dynamically at runtime.
 *
 * <h3>Retrieving for later use</h3>
 *
 * You can keep the {@link ChannelHandlerContext} for later use, such as
 * triggering an event outside the handler methods, even from a different thread.
 * todo  你可以获取一个  ChannelHandlerContext 供后续的使用, 比如说, 在这个handler之外触发一个事件 , 甚至是在不同的线程中触发事件
 *
 * <pre>
 * public class MyHandler extends {@link ChannelDuplexHandler} {
 *
 *     <b>private {@link ChannelHandlerContext} ctx;</b>
 *
 *     public void beforeAdd({@link ChannelHandlerContext} ctx) {
 *         <b>this.ctx = ctx;</b>
 *     }
 *
 *     public void login(String username, password) {
 *         ctx.write(new LoginMessage(username, password));
 *     }
 *     ...
 * }
 *
 * // todo 这就是一个提前保存好 ChannelHandlerContext 并后续使用的例子
 * </pre>
 *
 * <h3>Storing stateful information</h3>
 *  todo  存储有状态的信息
 *
 * {@link #attr(AttributeKey)} allow you to
 * store and access stateful information that is related with a handler and its
 * context. Please refer to {@link ChannelHandler} to learn various recommended
 * ways to manage stateful information.
 *
 * <h3>A handler can have more than one context</h3>
 * todo 一个handler 可以有多余一个的context
 *
 * Please note that a {@link ChannelHandler} instance can be added to more than
 * one {@link ChannelPipeline}.
 * todo 请注意, 一个chanelHandler实例允许多次被添加到 同一个ChannelPipeline中
 *
 * It means a single {@link ChannelHandler}
 * instance can have more than one {@link ChannelHandlerContext} and therefore
 * the single instance can be invoked with different
 * {@link ChannelHandlerContext}s if it is added to one or more
 * {@link ChannelPipeline}s more than once.
 * <p>
 *     todo  同一个Handler被多次添加到 channelPipeline中, 每添加一次就会生成一个 ChannelHandlerContext
 *
 *
 * For example, the following handler will have as many independent {@link AttributeKey}s
 * as how many times it is added to pipelines, regardless if it is added to the
 * same pipeline multiple times or added to different pipelines multiple times:
 * <pre> todo 如下的handler就会有多个 AttributeKey , 这取决于它被添加到管道的次数
 *
 * public class FactorialHandler extends {@link ChannelInboundHandlerAdapter} {
 *
 *   private final {@link AttributeKey}&lt;{@link Integer}&gt; counter = {@link AttributeKey}.valueOf("counter");
 *
 *   // This handler will receive a sequence of increasing integers starting
 *   // from 1.
 *   {@code @Override}
 *   public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     Integer a = ctx.attr(counter).get();
 *
 *     if (a == null) {
 *       a = 1;
 *     }
 *
 *     attr.set(a * (Integer) msg);
 *   }
 * }
 *
 * // Different context objects are given to "f1", "f2", "f3", and "f4" even if
 * // they refer to the same handler instance.  Because the FactorialHandler
 * // stores its state in a context object (using an {@link AttributeKey}), the factorial is
 * // calculated correctly 4 times once the two pipelines (p1 and p2) are active.
 * FactorialHandler fh = new FactorialHandler();
 *  todo  即便f1-f4 是四个不同的对象, 但是他们引用了相同的 handler实体,
 *  todo 但是呢, 每个handler被添加到pipeline中时,都会在ChannelHandlerContext中记录下它的状态, 所以依然会被计算 4次
 * {@link ChannelPipeline} p1 = {@link Channels}.pipeline();
 * p1.addLast("f1", fh);
 * p1.addLast("f2", fh);
 * // todo 换句话说, 上面同时往通道添加了相同的 handler 但是两个handler都会被执行
 * {@link ChannelPipeline} p2 = {@link Channels}.pipeline();
 * p2.addLast("f3", fh);
 * p2.addLast("f4", fh);
 * </pre>
 *
 * <h3>Additional resources worth reading</h3>
 * <p>
 * Please refer to the {@link ChannelHandler}, and
 * {@link ChannelPipeline} to find out more about inbound and outbound operations,
 * what fundamental differences they have, how they flow in a  pipeline,  and how to handle
 * the operation in your application.
 */
// todo AttributeMap -- 让ChannelHandlerContext 可以存储自定义的属性
// todo ChannelInboundInvoker -- 让ChannelHandlerContext 可以进行 InBound事件的传播,读事件,read 或者是  注册事件 active事件
// todo ChannelOutboundInvoker -- 让ChannelHandlerContext 可以传播写事件
public interface ChannelHandlerContext extends AttributeMap, ChannelInboundInvoker, ChannelOutboundInvoker {

    /**
     * Return the {@link Channel} which is bound to the {@link ChannelHandlerContext}.
     */
    // todo 获取ChannelHandlerContext所对应的这个Channel对象
    Channel channel();

    /**
     * Returns the {@link EventExecutor} which is used to execute an arbitrary task.
     */// todo 获取事件执行器
    EventExecutor executor();

    /**
     * The unique name of the {@link ChannelHandlerContext}.The name was used when then {@link ChannelHandler}
     * was added to the {@link ChannelPipeline}. This name can also be used to access the registered
     * {@link ChannelHandler} from the {@link ChannelPipeline}.
     */
    String name();

    /**
     * The {@link ChannelHandler} that is bound this {@link ChannelHandlerContext}.
     */
    // todo 获取ChannelHandlerContent对应的处理器对象
    ChannelHandler handler();

    /**
     * Return {@code true} if the {@link ChannelHandler} which belongs to this context was removed
     * from the {@link ChannelPipeline}. Note that this method is only meant to be called from with in the
     * {@link EventLoop}.
     * todo 表示这个节点是否被移除了
     */
    boolean isRemoved();

    // todo 下面的抽象方法, 是ChannelHandlerContext中定义的  事件传播函数
    @Override
    ChannelHandlerContext fireChannelRegistered();

    @Override
    ChannelHandlerContext fireChannelUnregistered();

    @Override
    ChannelHandlerContext fireChannelActive();

    @Override
    ChannelHandlerContext fireChannelInactive();

    @Override
    ChannelHandlerContext fireExceptionCaught(Throwable cause);

    @Override
    ChannelHandlerContext fireUserEventTriggered(Object evt);

    @Override
    ChannelHandlerContext fireChannelRead(Object msg);

    @Override
    ChannelHandlerContext fireChannelReadComplete();

    @Override
    ChannelHandlerContext fireChannelWritabilityChanged();

    @Override
    ChannelHandlerContext read();

    @Override
    ChannelHandlerContext flush();

    /**
     * Return the assigned {@link ChannelPipeline}
     * todo 获取当前节点的pipeline
     */
    ChannelPipeline pipeline();

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     * // todo 内存分配器, 当前节点中有数据时,我应该分配多大的内存
     */
    ByteBufAllocator alloc();

    /**
     * @deprecated Use {@link Channel#attr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * @deprecated Use {@link Channel#hasAttr(AttributeKey)}
     */
    @Deprecated
    @Override
    <T> boolean hasAttr(AttributeKey<T> key);
}
