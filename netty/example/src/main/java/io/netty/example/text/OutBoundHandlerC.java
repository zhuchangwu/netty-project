package io.netty.example.text;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Changwu
 * @Date: 2019/7/19 10:19
 */
public class OutBoundHandlerC extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println( "hello OutBoundHandlerC");
        ctx.write(msg, promise);
    }
}
