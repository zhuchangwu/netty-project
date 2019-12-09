package io.netty.example.myExamples.example5;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 19:14
 */
public class MyWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        System.out.println("收到消息: "+msg.text());
        // todo WebSocket能使用的数据都是XXXFrame, 要是下面发送的是Stirng ,其实是发送不出去的
        ctx.writeAndFlush(new TextWebSocketFrame(msg.text()));
    }
}
