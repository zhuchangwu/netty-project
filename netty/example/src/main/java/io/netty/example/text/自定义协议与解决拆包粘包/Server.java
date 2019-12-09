package io.netty.example.text.自定义协议与解决拆包粘包;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 21:32
 */
public class Server {
    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ChannelFuture sync = new ServerBootstrap().
                     group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new MyServerInitializer())
                    .bind(9999).sync();

            sync.channel().closeFuture().sync();

        } finally {
            worker.shutdownGracefully();
            boss.shutdownGracefully();
        }
    }
}
