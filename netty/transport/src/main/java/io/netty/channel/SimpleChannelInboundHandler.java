/*
 * Copyright 2013 The Netty Project
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

import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;

/**
 * {@link ChannelInboundHandlerAdapter} which allows to explicit only handle a specific type of messages.
 * // todo 它允许我们 只处理特定类型的消息
 * For example here is an implementation which only handle {@link String} messages.
 *  // todo 比如下面的实现,只会处理String消息, 因为它的泛型就是字符串类型的
 * <pre>
 *     public class StringHandler extends  SimpleChannelInboundHandler<  String >{
 *         {@code @Override}
 *         protected void channelRead0(  ChannelHandlerContext ctx, String message)
 *                 throws {@link Exception} {
 *             System.out.println(message);
 *         }
 *     }
 * </pre>
 *   todo 但是  ChannelInboundHandlerAdapter不是泛型类, 而是Object , 所以我们需要自己强制类型转换
 *
 * Be aware that depending of the constructor parameters it will release all handled messages by passing them to
 * {@link ReferenceCountUtil#release(Object)}.
 *    todo 请注意,根据构造方法参数的不同,她会释放所有的已经处理的消息,通过将这个消息传递给  ReferenceCountUtil#release(Object)
 *
 * In this case you may need to use
 * {@link ReferenceCountUtil#retain(Object)} if you pass the object to the next handler in the {@link ChannelPipeline}.
 *
 * <h3>Forward compatibility notice</h3>
 * <p>
 * Please keep in mind that {@link #channelRead0(ChannelHandlerContext, I)} will be renamed to
 * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.
 * </p>
 */
// todo 直接继承于ChanelInboundHandlerAdapter的实现 抽象类
// todo 我们自己的处理器, 同样可以继承SimpleChannelInboundHandler适配器,达到相同的效果
public abstract class SimpleChannelInboundHandler<I> extends ChannelInboundHandlerAdapter {

    private final TypeParameterMatcher matcher;
    private final boolean autoRelease;

    /**
     * see {@link #SimpleChannelInboundHandler(boolean)} with {@code true} as boolean parameter.
     */
    protected SimpleChannelInboundHandler() {
        this(true);
    }

    /**
     * Create a new instance which will try to detect the types to match out of the type parameter of the class.
     *
     * @param autoRelease   {@code true} if handled messages should be released automatically by passing them to
     *                      {@link ReferenceCountUtil#release(Object)}.
     */
    protected SimpleChannelInboundHandler(boolean autoRelease) {
        matcher = TypeParameterMatcher.find(this, SimpleChannelInboundHandler.class, "I");
        this.autoRelease = autoRelease;
    }

    /**
     * see {@link #SimpleChannelInboundHandler(Class, boolean)} with {@code true} as boolean value.
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType) {
        this(inboundMessageType, true);
    }

    /**
     * Create a new instance
     *
     * @param inboundMessageType    The type of messages to match
     * @param autoRelease           {@code true} if handled messages should be released automatically by passing them to
     *                              {@link ReferenceCountUtil#release(Object)}.
     */
    protected SimpleChannelInboundHandler(Class<? extends I> inboundMessageType, boolean autoRelease) {
        matcher = TypeParameterMatcher.get(inboundMessageType);
        this.autoRelease = autoRelease;
    }

    /**
     * Returns {@code true} if the given message should be handled. If {@code false} it will be passed to the next
     * {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     */
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return matcher.match(msg);
    }

    // todo  channelRead 完全被改写了
    // todo 这其实又是一种设计模式 ,   模板方法设计模式
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean release = true;
        try {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                // todo 把消息进行了强转
                I imsg = (I) msg;
                //  todo channelRead0()在他的父类中是抽象的,因此我们自己写handler时,需要重写它的这个抽象的 方法 , 在下面
                // todo 这其实又是一种设计模式 ,   模板方法设计模式
                channelRead0(ctx, imsg);
            } else {
                release = false;
                ctx.fireChannelRead(msg);
            }
        } finally {// todo 对msg的计数减一, 表示对消息的引用减一. 也就意味着我们不要在任何
            if (autoRelease && release) {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    /**
     * <strong>Please keep in mind that this method will be renamed to
     * {@code messageReceived(ChannelHandlerContext, I)} in 5.0.</strong>
     *
     * Is called for each message of type {@link I}.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     * @throws Exception    is thrown if an error occurred
     */
    protected abstract void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception;
}
