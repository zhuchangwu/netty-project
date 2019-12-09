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

import io.netty.util.internal.StringUtil;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} implementation with a simple naming rule.
 */
// todo 简单实现ThreadFactory  , 并且提供了命名规则
public class DefaultThreadFactory implements ThreadFactory {

    private static final AtomicInteger poolId = new AtomicInteger();

    private final AtomicInteger nextId = new AtomicInteger();
    private final String prefix;
    private final boolean daemon;
    // todo 线程的优先级
    private final int priority;
    protected final ThreadGroup threadGroup;

    public DefaultThreadFactory(Class<?> poolType) {
        // todo 线程的优先级, 使用的是jdk底层的优先级, 1 - 5 - 10
        this(poolType, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName) {
        this(poolName, false, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, boolean daemon) {
        this(poolType, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(String poolName, boolean daemon) {
        this(poolName, daemon, Thread.NORM_PRIORITY);
    }

    public DefaultThreadFactory(Class<?> poolType, int priority) {
        this(poolType, false, priority);
    }

    public DefaultThreadFactory(String poolName, int priority) {
        this(poolName, false, priority);
    }

    // todo 来这里                                            false
    public DefaultThreadFactory(Class<?> poolType, boolean daemon, int priority) {
        // todo 线程  现在他不是后台线程  优先级是5, 就在下面
        this(toPoolName(poolType), daemon, priority);
    }

    public static String toPoolName(Class<?> poolType) {
        if (poolType == null) {
            throw new NullPointerException("poolType");
        }

        String poolName = StringUtil.simpleClassName(poolType);
        switch (poolName.length()) {
            case 0:
                return "unknown";
            case 1:
                return poolName.toLowerCase(Locale.US);
            default:
                if (Character.isUpperCase(poolName.charAt(0)) && Character.isLowerCase(poolName.charAt(1))) {
                    return Character.toLowerCase(poolName.charAt(0)) + poolName.substring(1);
                } else {
                    return poolName;
                }
        }
    }

    // todo 第三步来到这, 初始化了本类的一些状态
    public DefaultThreadFactory(String poolName, boolean daemon, int priority, ThreadGroup threadGroup) {
        if (poolName == null) {
            throw new NullPointerException("poolName");
    }
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }

        prefix = poolName + '-' + poolId.incrementAndGet() + '-'; // todo 继续设置线程的名字
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
        // todo 下面去看  线程工厂的  newThread方法, 在下面
    }


    // todo  在来到这
    public DefaultThreadFactory(String poolName, boolean daemon, int priority) {
        this(poolName, daemon, priority, System.getSecurityManager() == null ?
                Thread.currentThread().getThreadGroup() : System.getSecurityManager().getThreadGroup());
    }

    @Override
    public Thread newThread(Runnable r) {
        // todo DefaultRunnableDecorator 是DefaultThreadFactory的内部类
        // todo newThread()的重载方法在下面
        Thread t = newThread(new DefaultRunnableDecorator(r), prefix + nextId.incrementAndGet());
        try {
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) {
            // Doesn't matter even if failed to set.
        }
        return t;
    }

    // todo 可以看到,Netty底层使用的Thread,并不是原生的Thread, 而是他自己从新封装的Thread
    protected Thread newThread(Runnable r, String name) {
        // todo threadGroup是
        System.out.println(threadGroup+"   threadGroup");
        return new FastThreadLocalThread(threadGroup, r, name);
    }


    // todo 装饰 runable, 可以看到,其实他的Run方法, 直接调用 runable的Run方法, 而不需要  把Runable放在Thread中再 start
    private static final class DefaultRunnableDecorator implements Runnable {

        private final Runnable r;

        DefaultRunnableDecorator(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } finally {
                FastThreadLocal.removeAll();
            }
        }
    }
}
