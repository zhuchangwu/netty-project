package io.netty.example.myExamples.example6;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;

/**
 * @Author: Changwu
 * @Date: 2019/12/9 10:11
 */
public class MyInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 业务线程池
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(20);
       // pipeline.addLast(eventLoopGroup,new MyHandler());
    }
}
