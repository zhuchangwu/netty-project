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

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 *  todo 这里实际上使用了设计模式
 *  todo 1. command是用户定义的任务, 命令模式; 直观的 我定义一种任务, 程序不需要知道我执行的命令是什么,但是当我把任务扔给你, 你帮我执行就好了
 *  todo 2. 代理设计模型, 代理了ThreadFactory  , 把本来给ThreadPerTaskExecutor执行的任务给了ThreadFactory
 */
public final class ThreadPerTaskExecutor implements Executor {
    private final ThreadFactory threadFactory;

    public ThreadPerTaskExecutor(ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        this.threadFactory = threadFactory;
    }

    // todo  必须实现 Executor 里面唯一的抽象方法, execute , 执行性 任务
    @Override
    public void execute(Runnable command) {
        threadFactory.newThread(command).start();
    }
}
