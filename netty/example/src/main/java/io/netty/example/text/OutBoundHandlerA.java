package io.netty.example.text;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * @Author: Changwu
 * @Date: 2019/7/19 10:19
 */
public class OutBoundHandlerA extends ChannelOutboundHandlerAdapter {
    // todo  当服务端的channel绑定上端口之后,就是 传播 channelActive 事件
    // todo   事件传播到下面后,我们手动传播一个 channelRead事件
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println( "hello OutBoundHandlerA");
        ctx.write(ctx, promise);
    }
}
