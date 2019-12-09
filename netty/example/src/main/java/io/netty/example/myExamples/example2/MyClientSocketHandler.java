package io.netty.example.myExamples.example2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 16:05
 */
public class MyClientSocketHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("收到了服务端的消息： "+msg);
        ctx.channel().writeAndFlush("你好, Socket Server");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("客户端的channel active");
        ctx.channel().writeAndFlush("hello socket server");
    }
}
