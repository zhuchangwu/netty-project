package io.netty.example.text;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @Author: Changwu
 * @Date: 2019/7/19 10:19
 */
public class MyServerHandlerA extends ChannelInboundHandlerAdapter {
    // todo  当服务端的channel绑定上端口之后,就是 传播 channelActive 事件
    // todo   事件传播到下面后,我们手动传播一个 channelRead事件
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().pipeline().fireChannelRead("hello MyServerHandlerA");
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("MyServerHandlerA  channelRead");
        ctx.fireChannelRead(msg);
    }
}
