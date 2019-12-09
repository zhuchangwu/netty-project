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
import io.netty.util.concurrent.BlockingOperationException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.TimeUnit;


/**
 * The result of an asynchronous {@link Channel} I/O operation.
 * todo 表示一个Channel的异步的IO操作的结果
 * <p>
 * All I/O operations in Netty are asynchronous.  It means any I/O calls will
 * return immediately with no guarantee that the requested I/O operation has
 * been completed at the end of the call.
 *  todo netty中所有的IO操作都是异步的, 这意味着,方法一旦调用就会返回, 所有不能保证所有的操作都已经完成
 *
 * Instead, you will be returned with
 * a {@link ChannelFuture} instance which gives you the information about the
 * result or status of the I/O operation.
 *  todo  相反, 它会向你返回一个future对象, 包含了这些IO操作的一些结果和状态的一些信息
 * <p>
 * A {@link ChannelFuture} is either <em>uncompleted</em> or <em>completed</em>.
 * todo 一个ChannelFuture 要么是完成的,要么是未完成的
 *
 * When an I/O operation begins, a new future object is created.  The new future
 * is uncompleted initially - it is neither succeeded, failed, nor cancelled
 * because the I/O operation is not finished yet.
 * todo 当一个IO操作开始的时候, 一个新的未完成的Futrue对象就会被创建出来, 这个future对象 既不是成功的,也不是失败的,更不是被取消的 ,因为这个IO还没结束
 *
 * If the I/O operation is
 * finished either successfully, with failure, or by cancellation, the future is
 * marked as completed with more specific information, such as the cause of the
 * failure.  Please note that even failure and cancellation belong to the
 * completed state.
 *  todo 当一个IO操作 成功的完成了, 或是失败的完成了, 亦或是 被取消了, 它都被当作是  completed 完成了
 * <pre>
 *                                      +---------------------------+
 *                                      | Completed successfully    |
 *                                      +---------------------------+
 *                                 +---->      isDone() = true      |
 * +--------- -----------------+    |    |   isSuccess() = true      |
 * |        Uncompleted       |    |    +===========================+
 * +--------------------------+    |    | Completed with failure    |
 * |      isDone() = false    |    |    +---------------------------+
 * |   isSuccess() = false    |----+---->      isDone() = true      |
 * | isCancelled() = false    |    |    |       cause() = non-null todo 非空 |
 * |       cause() = null     |    |    +===========================+
 * +--------------------------+    |    | Completed by cancellation |
 *                                 |    +---------------------------+
 *                                 +---->      isDone() = true      |
 *                                      | isCancelled() = true      |
 *                                      +---------------------------+
 * </pre>
 *
 * todo 图解, 未完成的状态 ---> 三种可能的完成的状态
 *
 * Various methods are provided to let you check if the I/O operation has been
 * completed, wait for the completion, and retrieve the result of the I/O
 * operation. It also allows you to add {@link ChannelFutureListener}s so you
 * can get notified when the I/O operation is completed.
 * todo 各种各样的方法提供给你去检查IO是否完成了,等待结果,接受结果
 * todo  同样, 也可以添加ChannelFutrue Listener去监听IO操作, 这样只要IO操作一旦完成,立刻就会收到通知
 *
 *
 * <h3>Prefer {@link #addListener(GenericFutureListener)} to {@link #await()}</h3>
 * todo 推荐使用的是 addListenner去监听  而不是使用await()方法
 *
 *
 * It is recommended to prefer {@link #addListener(GenericFutureListener)} to
 * {@link #await()} wherever possible to get notified when an I/O operation is
 * done and to do any follow-up tasks.
 *  todo 推荐使用的是 addListenner去监听 收到通知,顺势进行后续的任务处理
 * <p>
 * {@link #addListener(GenericFutureListener)} is non-blocking.  It simply adds
 * the specified {@link ChannelFutureListener} to the {@link ChannelFuture}, and
 *  * I/O thread will notify the listeners when the I/O operation associated with
 *  * the future is done.  {@link ChannelFutureListener} yields the best
 *  * performance and resource utilization because it does not block at all, but
 *  * it could be tricky to implement a sequential logic if you are not used to
 *  * event-driven programming.
 * <p>
 *  todo addListener 非阻塞,但是 实现一个顺序的逻辑, 如果你还不熟悉怎么使用 事件驱动的编程模式
 *
 * By contrast, {@link #await()} is a blocking operation.  Once called, the
 * caller thread blocks until the operation is done.  It is easier to implement
 * a sequential logic with {@link #await()}, but the caller thread blocks
 * unnecessarily until the I/O operation is done and there's relatively
 * expensive cost of inter-thread notification.  Moreover, there's a chance of
 * dead lock in a particular circumstance, which is described below.
 * todo  与之相反的是 await 是阻塞的, 比较容易的实现 顺序逻辑编程,但是一旦卡住了,效率就低了, 还可能出现死锁
 *
 * <h3>Do not call {@link #await()} inside {@link ChannelHandler}</h3>
 * <p>
 * todo 不要在 ChannelHandler中调用await()方法
 *
 * The event handler methods in {@link ChannelHandler} are usually called by
 * an I/O thread.  If {@link #await()} is called by an event handler
 * method, which is called by the I/O thread, the I/O operation it is waiting
 * for might never complete because {@link #await()} can block the I/O
 * operation it is waiting for, which is a dead lock.
 * <pre>
 * todo ChannelHandler中的处理方法通常是被 IO 线程调用, 如果在他里面调用了 await() 方法, 可能就会阻塞IO线程中的操作形成死锁
 *
 * // BAD - NEVER DO THIS
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *      future.awaitUninterruptibly();
 *      todo 永远不要这样做
 *     // Perform post-closure operation
 *     // ...
 * }
 * todo  这是个不好的例子, 在channelRead() 读取客户端的消息
 *
 *
 * // GOOD
 * {@code @Override}
 * public void channelRead({@link ChannelHandlerContext} ctx, Object msg) {
 *     {@link ChannelFuture} future = ctx.channel().close();
 *     future.addListener(new {@link ChannelFutureListener}() {
 *         public void operationComplete({@link ChannelFuture} future) {
 *         todo 推荐这样写
 *             // Perform post-closure operation
 *             // ...
 *         }
 *     });
 * }
 * </pre>
 * <p>
 * In spite of the disadvantages mentioned above, there are certainly the cases
 * where it is more convenient to call {@link #await()}. In such a case, please
 * make sure you do not call {@link #await()} in an I/O thread.  Otherwise,
 * {@link BlockingOperationException} will be raised to prevent a dead lock.
 * todo 尽管上面说了很多await()方法的不足之处, 如果真的有必要使用await() , 请确保不要在IO线程里调用它
 *
 *
 * <h3>Do not confuse I/O timeout and await timeout</h3>
 * todo 不要将IO超时和等待超时混为一滩
 *
 * The timeout value you specify with {@link #await(long)},
 * {@link #await(long, TimeUnit)}, {@link #awaitUninterruptibly(long)}, or
 * {@link #awaitUninterruptibly(long, TimeUnit)} are not related with I/O
 * timeout at all.
 *todo await() 超时和 IO超时没关系
 *
 * If an I/O operation times out, the future will be marked as
 * 'completed with failure,' as depicted in the diagram above.  For example,
 * connect timeout should be configured via a transport-specific option:
 * <pre>
 * todo 如果出来了io超时, 这个Future会被标识为 失败 , 推荐可能出现的连接超时 应该配置一个 transport-specific
 *
 *
 *
 * //todo  BAD - NEVER DO THIS
 * {@link Bootstrap} b = ...;
 * {@link ChannelFuture} f = b.connect(...);
 * todo   b.connect(...); 返回一个 ChannelFuture
 * todo   方法是异步的, 直接返回了,往下执行时,很可能连接还没有建立上 下面使用的空对象的时候,就可能出现空指针异常
 *
 * f.awaitUninterruptibly(10, TimeUnit.SECONDS);
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     // You might get a NullPointerException here because the future
 *     // might not be completed yet.
 *     f.cause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 *
 *
 *
 * // todo GOOD
 * {@link Bootstrap} b = ...;
 * // Configure the connect timeout option.
 * <b>b.option({@link ChannelOption}.CONNECT_TIMEOUT_MILLIS, 10000);</b>
 * {@link ChannelFuture} f = b.connect(...);
 *  todo 这里的连接可以出现超时, 但是之前我们配置了链接超时的可选项   b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
 * f.awaitUninterruptibly();
 * // Now we are sure the future is completed.
 * assert f.isDone();
 * todo 断言,肯定完成了, 保证连接一定建立了
 *
 * if (f.isCancelled()) {
 *     // Connection attempt cancelled by user
 * } else if (!f.isSuccess()) {
 *     f.cause().printStackTrace();
 * } else {
 *     // Connection established successfully
 * }
 * </pre>
 */

//todo  直接继承自己的Futrue接口,  间接继承了 jdk1.5的Future , 又添加了若干方法
public interface ChannelFuture extends Future<Void> {

    /**
     * Returns a channel where the I/O operation associated with this
     *      * future takes place.
     * todo 返回 触发本次I/O操作的  futureChanel 未来通道。 实际上就是返回当前的客户端
     */
    Channel channel();

    @Override
    ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);

    @Override
    ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener);

    @Override
    ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners);
// todo 定义在netty的Futrue接口中
    @Override
    ChannelFuture sync() throws InterruptedException;

    @Override
    ChannelFuture syncUninterruptibly();

    @Override
    ChannelFuture await() throws InterruptedException;

    @Override
    ChannelFuture awaitUninterruptibly();

    /**
     * Returns {@code true} if this {@link ChannelFuture} is a void future and so not allow to call any of the
     * following methods:
     * <ul>
     *     <li>{@link #addListener(GenericFutureListener)}</li>
     *     <li>{@link #addListeners(GenericFutureListener[])}</li>
     *     <li>{@link #await()}</li>
     *     <li>{@link #await(long, TimeUnit)} ()}</li>
     *     <li>{@link #await(long)} ()}</li>
     *     <li>{@link #awaitUninterruptibly()}</li>
     *     <li>{@link #sync()}</li>
     *     <li>{@link #syncUninterruptibly()}</li>
     * </ul>
     */
    boolean isVoid();
}
