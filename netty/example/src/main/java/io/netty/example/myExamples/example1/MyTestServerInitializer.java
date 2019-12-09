package io.netty.example.myExamples.example1;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;


/**
 * 初始化器
 * @Author: Changwu
 * @Date: 2019/12/7 13:44
 */
public class MyTestServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // todo
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new MyHttpServerHandler());
    }
}
