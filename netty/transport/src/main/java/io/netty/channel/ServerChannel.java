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

import io.netty.channel.socket.ServerSocketChannel;

/**
 * A {@link Channel} that accepts an incoming connection attempt and creates
 * its child {@link Channel}s by accepting them.  {@link ServerSocketChannel} is
 * a good example.
 * todo  ServerChannel  会接受一个请求,并请通过 accept() 方法, 创建出 childChanel
 * todo ServerSocketChannel 就是一个很好的例子
 *
 */
// todo  ServerChannel 是一个标识接口, 实现它,就有我们约定的形式
// todo  其中NioServerSocketChannel就是一个特别重要的服务端的实现类
public interface ServerChannel extends Channel {
    // This is a tag interface.
}
