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

import java.util.EventListener;

/**
 * Listens to the result of a {@link Future}.  The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#addListener(GenericFutureListener)}.
 */
// todo  监听 future的异步操作的结果, 当Futrue使用 addListener(GenericFutureListener) 添加了监听器
    // todo F extends Future<?>   F的类型被限制于 Future及其下面的类型
public interface GenericFutureListener<F extends Future<?>> extends EventListener { //todo  通知   是Java.Util包下面的 EventListener

    /**
     * Invoked when the operation associated with the {@link Future} has been completed.
     *
     * @param future  the source {@link Future} which called this callback
     */
    // todo future的异步操作完成的时被自动调用,
    // todo  F future 入参位置的Future是调用了参数回调的源future  , 相当于把 主题对象 future传递给了观察者对象
     void operationComplete(F future) throws Exception;
}
