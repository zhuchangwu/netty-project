package io.netty.example.text;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @Author: Changwu
 * @Date: 2019/7/19 10:19
 */
public class MyServerHandlerC extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("MyServerHandlerC  channelRead");
        ctx.fireChannelRead(msg);
    }
}
