package io.netty.example.text.自定义协议与解决拆包粘包;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 21:30
 */
public class MyServerInitializer extends ChannelInitializer {
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new MyPersonDecoder());
        pipeline.addLast(new MyPersonEncoder());
        pipeline.addLast(new MyServerHandler());
    }
}
