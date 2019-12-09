package io.netty.example.text;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Changwu
 * @Date: 2019/7/19 10:19
 */
public class OutBoundHandlerB extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println( "hello OutBoundHandlerB");
        ctx.write(ctx, promise);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("被回调了....");
        ctx.executor().schedule(()->{
            // todo 模拟给 客户端一个响应
            ctx.channel().write("Hello World");
            // 写法二 :  ctx.write("Hello World");
        },3, TimeUnit.SECONDS);
    }
}
