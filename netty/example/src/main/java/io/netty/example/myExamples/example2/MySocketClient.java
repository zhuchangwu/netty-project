package io.netty.example.myExamples.example2;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 15:55
 */
public class MySocketClient {
    public static void main(String[] args) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new MyClientSocketInitializer());

            System.out.println("-----------准备启动-------------");
            ChannelFuture future = bootstrap.connect("localhost", 8888).sync();
            System.out.println("-----------启动完成-------------");
            System.out.println("------------准备关闭------------");
            future.channel().closeFuture().sync();
            System.out.println("------------完成关闭------------");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }


    }
}
