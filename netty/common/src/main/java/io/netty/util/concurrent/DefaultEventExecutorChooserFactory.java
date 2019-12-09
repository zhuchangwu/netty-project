/*
 * Copyright 2016 The Netty Project
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

import io.netty.util.internal.UnstableApi;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation which uses simple round-robin to choose next {@link EventExecutor}.
 */
    // todo  使用简单的  轮询算法  在 Executor数组中 选择出 下一个被使用的 EventExecutor.
    // todo  补充, 当样本超级大的时候, random  可以和round-robin达到相同的效果
@UnstableApi
public final class DefaultEventExecutorChooserFactory implements EventExecutorChooserFactory {

    public static final DefaultEventExecutorChooserFactory INSTANCE = new DefaultEventExecutorChooserFactory();

    private DefaultEventExecutorChooserFactory() { }

    @SuppressWarnings("unchecked")
    @Override
    // todo 创建选择器实现
    public EventExecutorChooser newChooser(EventExecutor[] executors) {
        // todo 下面的两种  事件执行器选择器是当前类的两个内部类, 分别定义了next() 用于获取下一个Executor

        if (isPowerOfTwo(executors.length)) {// todo 如果是2的指数倍, 返回PowerOfTwoEventExecutorChooser
            return new PowerOfTwoEventExecutorChooser(executors);
        } else {// todo  否则返回同样的实例
            return new GenericEventExecutorChooser(executors);
        }
    }

    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }


    private static final class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {
        private final AtomicInteger idx = new AtomicInteger();
        private final EventExecutor[] executors;

        PowerOfTwoEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            //todo 是先减 再&
            /**
             * todo   idx                       111010
             * todo                                 &
             * todo   executor.lenth-1            1111
             * todo   result                      1010
             */
            return executors[idx.getAndIncrement() & executors.length - 1];
        }
    }

    private static final class GenericEventExecutorChooser implements EventExecutorChooser {
        private final AtomicInteger idx = new AtomicInteger();
        private final EventExecutor[] executors;

        GenericEventExecutorChooser(EventExecutor[] executors) {
            this.executors = executors;
        }

        @Override
        public EventExecutor next() {
            // todo 从0开始到最后一个, 再从零开始,到最后一个
            return executors[Math.abs(idx.getAndIncrement() % executors.length)];
        }
    }
}
