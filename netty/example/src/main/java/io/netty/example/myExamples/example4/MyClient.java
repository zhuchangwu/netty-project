package io.netty.example.myExamples.example4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 17:12
 */
public class MyClient {
    public static void main(String[] args) {

        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new MyClientInitializer());

        try {
            Channel channel= bootstrap.connect("localhost", 8888).sync().channel();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                channel.writeAndFlush(bufferedReader.readLine()+"\r\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
}
