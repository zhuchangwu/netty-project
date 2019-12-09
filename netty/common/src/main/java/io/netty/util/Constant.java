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
package io.netty.util;

/**
 * A singleton which is safe to compare via the {@code ==} operator. Created and managed by {@link ConstantPool}. todo 进入 ConstantPool
 */
// todo 他是单例的,并且可以安全的使用 = 进行比较,   由常量池创建管理
public interface Constant<T extends Constant<T>> extends Comparable<T> {

    /**
     * Returns the unique number assigned to this {@link Constant}.
     */
    // todo 这个常量的唯一身份id
    int id();

    /**
     * Returns the name of this {@link Constant}.
     */
    // todo 返回常量的名字
    String name();
}
