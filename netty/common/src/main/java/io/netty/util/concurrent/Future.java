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
package io.netty.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

/**
 *  todo netty为什么会提供 一个自己的Future　？　
 *  todo java原生的 Future的get() 方法是有可能导致程序的阻塞的, 在netty的Futrue中添加了监听器, 一旦定义的Future任务有返回, 监听器会被通知,再去get()
 *  // todo 规避了 程序阻塞的情况
 *
 */

/**
 *  todo 看一下,原生Futrue中提供的例子, 异步提交任务, 得到Future对象
 *      ExecutorService executor = ...
 *  *   ArchiveSearcher searcher = ...
 *
 *  *   void showSearch(final String target)
 *  *       throws InterruptedException {
 *  *     Future<String> future
 *  *       = executor.submit(new Callable<String>() {
 *  *         public String call() {
 *  *             return searcher.search(target);
 *  *         }});
 *  *     displayOtherThings(); // do other things while searching
 *  *     try {
 *  *       displayText(future.get()); // use future
 *  *     } catch (ExecutionException ex) { cleanup(); return; }
 *  *   }
 *
 *
 *  // todo 创建 FutureTask 对象, 接受Callable参数, 返回future
 *  FutureTask<String> future =
 *  *   new FutureTask<String>(new Callable<String>() {
 *  *     public String call() {
 *  *       return searcher.search(target);
 *  *   }});
 *  * executor.execute(future);}</pre>
 */


/**
 * The result of an asynchronous operation.
 */
@SuppressWarnings("ClassNameSameAsAncestorName")
// todo 这个接口继承了 java并发包总的Futrue  , 并在其基础上增加了很多方法
// todo  Future 表示对未来任务的封装
public interface Future<V> extends java.util.concurrent.Future<V> {

    /**
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.
     */
    // todo 判断IO是否成功返回
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     */
    // todo 判断是否是 cancel()方法取消
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has
     * failed.
     *
     * @return the cause of the failure.
     *         {@code null} if succeeded or this future is not
     *         completed yet.
     */
    // todo 返回IO 操作失败的原因
    Throwable cause();

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listener is notified immediately.
     */
    /**
     *  todo 使用了观察者设计模式, 给这个future添加监听器, 一旦Future 完成, listenner 立即被通知
     * @param listener
     * @return
     */
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listeners are notified immediately.
     */
    // todo 添加多个listenner
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     */
    // todo 移除listenner
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listeners are not associated with this future, this method
     * does nothing and returns silently.
     */
    // todo 移除多个 listenner
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */
    // todo  sync(同步) 等待着 future 的完成, 并且,一旦future失败了,就会抛出 future 失败的原因
    // todo bind()是个异步操作,我们需要同步等待他执行成功
    Future<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     */
    // todo  不会被中断的 sync等待
    Future<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    // todo 等待
    Future<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without
     * interruption.  This method catches an {@link InterruptedException} and
     * discards it silently.
     */
    Future<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     *
     * @throws InterruptedException
     *         if the current thread was interrupted
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     *
     * @return {@code true} if and only if the future was completed within
     *         the specified time limit
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not relay on the returned {@code null} value.
     */
    // todo 无阻塞的返回Future对象, 如果没有,返回null
    // todo  有时 future成功执行后返回值为null, 这是null就是成功的标识, 如 Runable就没有返回值, 因此文档建议还要 通过isDone() 判断一下真的完成了吗

    V getNow();

    /**
     * {@inheritDoc}
     *
     * If the cancellation was successful it will fail the future with an {@link CancellationException}.
     */
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
