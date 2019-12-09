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
package io.netty.channel.socket.nio;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.util.internal.SocketUtils;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DefaultServerSocketChannelConfig;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

/**
 * A {@link io.netty.channel.socket.ServerSocketChannel} implementation which uses
 * NIO selector based implementation to accept new connections.
 */
// todo 下面两行是上注册的翻译,但是我觉的不准确, Selector是事件循环组, 而现在的NioServerSocketChannel则是需要注册进事件循环组的被监听的对象
// todo 它 实际上就是 NIO Selector 选择器的一种实现, 会不断的 accept 接受新的连接
// todo 在 创建出 ServerSocketChannel , 设置它 非阻塞, 绑定 感兴趣的事件
public class NioServerSocketChannel extends AbstractNioMessageChannel
                             implements io.netty.channel.socket.ServerSocketChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
  // todo SelectorProvider.provider();
    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioServerSocketChannel.class);

    // todo 通过jdk创建nio底层的serverSocketChannel
    private static ServerSocketChannel newSocket(SelectorProvider provider) {
        try {
            /**
             *  Use the {@link SelectorProvider} to open {@link SocketChannel} and so remove condition in
             *  {@link SelectorProvider#provider()} which is called by each ServerSocketChannel.open() otherwise.
             *
             *  See <a href="https://github.com/netty/netty/issues/2308">#2308</a>.
             */
            //todo 不再使用 provider.open() 获取 ServerSocketChannel , 据说当一秒5000并发的时候,会出现可能出现阻塞, 性能下降1%
            return provider.openServerSocketChannel();
        } catch (IOException e) {
            throw new ChannelException(
                    "Failed to open a server socket.", e);
        }
    }
    // todo 这个是netty里面的一个核心的组件,专门用于 封装配置消息的
    // todo 这个config 维护的就是我们设置进去的ChannelOption , 我们在初始化channel  init()方法时, 设置的option attr这些属性,全部在 NioServerSocketChannel的成员变量里面
    private final ServerSocketChannelConfig config;

    /**
     * todo Create a new instance
     */
    public NioServerSocketChannel() {
        // todo 使用 newSocket 创建出JDK原生的服务端使用的channel，仔细想想， Netty的NioServerSocketChannel其实就是对 ServerSocketChannel的封装
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }

    /**
     * Create a new instance using the given {@link SelectorProvider}.
     */
    public NioServerSocketChannel(SelectorProvider provider) {
        this(newSocket(provider));
    }

    /**
     * Create a new instance using the given {@link ServerSocketChannel}.
     */
    //todo NioServerSocketChannel 添加了感兴趣事件是  OP_ACCEPT 接受连接
    // todo 服务端启动的时候,主Reactor 唯一需要关注的事情就是  接受连接
    public NioServerSocketChannel(ServerSocketChannel channel) {
        //  todo 跟进去
        super(null, channel, SelectionKey.OP_ACCEPT);
        //  todo 通过追踪, javaChannel() 获取的就是,我们通过上一行的supper传递进去的 ServerSocketChannel
        //  todo 调用socket方法,返回值就是 ServerSocket 实例--- > 可能去绑定端口
        // todo NioServerSocketChannelConfig 是Tcp配置参数类
        config = new NioServerSocketChannelConfig(this, javaChannel().socket());
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public ServerSocketChannelConfig config() {
        return config;
    }

    @Override
    public boolean isActive() {
        return javaChannel().socket().isBound();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    protected ServerSocketChannel javaChannel() {
        // todo 跟进去调用supper的javachannel
        return (ServerSocketChannel) super.javaChannel();
    }

    @Override
    protected SocketAddress localAddress0() {
        return SocketUtils.localSocketAddress(javaChannel().socket());
    }

    // todo 真正的绑定操作
    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        if (PlatformDependent.javaVersion() >= 7) {
            javaChannel().bind(localAddress, config.getBacklog());
        } else {
            javaChannel().socket().bind(localAddress, config.getBacklog());
        }
    }

    @Override
    protected void doClose() throws Exception {
        javaChannel().close();
    }

    //todo doReadMessage  其实就是 doChannel
    // todo 处理新连接, 现在是在 NioServReaderSocketChannel里面
    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        // todo java Nio底层在这里 创建jdk底层的 原生channel
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {
                // todo  把java原生的channel, 封装成 Netty自定义的封装的channel , 这里的buf是list集合对象,由上一层传递过来的
                // todo  this  --  NioServerSocketChannel
                // todo  ch --     SocketChnnel
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                logger.warn("Failed to close a socket.", t2);
            }
        }

        return 0;
    }

    // Unnecessary stuff
    @Override
    protected boolean doConnect(
            SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doFinishConnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return null;
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) throws Exception {
        throw new UnsupportedOperationException();
    }

    // todo NioServerSocketChannelConfig 本身就是NioServerSocketChannel的私有的内部类
    private final class NioServerSocketChannelConfig extends DefaultServerSocketChannelConfig {
        // todo 参数被传递到这里  NioServerSocketChannel    ServerSocket
        private NioServerSocketChannelConfig(NioServerSocketChannel channel, ServerSocket javaSocket) {
            //            // todo 传递给他的父类
            super(channel, javaSocket);
        }

        @Override
        protected void autoReadCleared() {
            clearReadPending();
        }
    }
}
