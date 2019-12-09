package io.netty.example.myExamples.example4;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 18:30
 */
public class MyInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // todo 添加nettry原生的空闲状态检验的处理器
        pipeline.addLast(new IdleStateHandler(5,7,10, TimeUnit.SECONDS));

        // todo 自定义处理器，专门用来检验  处理器的netty的空闲状态检验处理器
        pipeline.addLast(new MyServerHandler());

    }
}
