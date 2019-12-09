package io.netty.example.text.引入tcp的黏包问题;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @Author: Changwu
 * @Date: 2019/12/9 13:09
 */
public class MyServer {
    public static void main(String[] args) throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup(10);

        try {
            ChannelFuture sync = new ServerBootstrap()
                    .group(boss, worker)
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
