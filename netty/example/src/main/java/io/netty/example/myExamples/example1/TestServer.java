package io.netty.example.myExamples.example1;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 第一件事: 像Tomcat一样, 作为Http服务器,接受http请求, 做出响应, 只不过没有实现Servelt规范
 *
 * 写一个http服务器,接收到客户端的http请求后, 返回给客户端 hello world
 *
 * @Author: Changwu
 * @Date: 2019/12/7 13:40
 */
public class TestServer {
    public static void main(String[] args) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(10);

        ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MyTestServerInitializer());

        try {
            ChannelFuture future = serverBootstrap.bind(9999).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
