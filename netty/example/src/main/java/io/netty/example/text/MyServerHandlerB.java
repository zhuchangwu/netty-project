package io.netty.example.text;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author: Changwu
 * @Date: 2019/7/19 10:19
 */
public class MyServerHandlerB extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("MyServerHandlerB  channelRead");
        ctx.fireChannelRead(msg);
    }
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

    }
}
