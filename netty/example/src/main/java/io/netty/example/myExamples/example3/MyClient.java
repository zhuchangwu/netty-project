package io.netty.example.myExamples.example3;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 17:12
 */
public class MyClient {
    public static void main(String[] args) {

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap  .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new MyClientInitializer());

        try {
            Channel channel= bootstrap.connect("localhost", 9999).sync().channel();
            System.out.println("-------------");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                System.out.println("-------------");
                channel.writeAndFlush(bufferedReader.readLine()+"\r\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
