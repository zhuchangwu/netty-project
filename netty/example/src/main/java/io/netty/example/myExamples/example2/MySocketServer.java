package io.netty.example.myExamples.example2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;

/**
 * todo 第二件事, socket的开发与自定义协议
 * Netty可是实现的第二件事就是Socket编程,也是它最为广泛的应用领域
 * 也是进行微服务开发时不可丢弃的一个点, 服务和服务之间如果使用Http通信不是不行,
 * 但是http的底层使用的也是socket, 相对我们直接使用netty加持socket效果会更好  (Dubbo)
 * <p>
 * todo 第三件事: 进行长连接的开发, Netty对WebSocket的支持
 *
 * @Author: Changwu
 * @Date: 2019/12/7 15:27
 */
public class MySocketServer {
    public static void main(String[] args) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(10);

        //todo ---  channelHandler 和 handler的区别
        //todo ---  handler 处理的是bossGroup 里面的事件
        //todo ---  childerHandler 处理的是workerGroup里面的事件
        ServerBootstrap bootstrap = new ServerBootstrap();
        ChannelFuture future = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler())
                .childHandler(new MyServerSockerInitializer())
                .bind(8888);
        try {
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }


    }


}
