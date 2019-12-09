package io.netty.example.text;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.UUID;

/**
 *
 * @Author: Changwu
 * @Date: 2019/6/10 16:15
 */
public class MyClientHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("服务器端的ip+端口 "+ctx.channel().remoteAddress());
        System.out.println("服务器返回: "+msg);

        ChannelFuture channelFuture = ctx.writeAndFlush("from client : " + UUID.randomUUID());
        channelFuture.addListener(future->{
            if(future.isSuccess()){

            }else{

            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("111");
        ctx.writeAndFlush("来自客户端...");
        //   super.channelActive(ctx);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    // 使用active,让客户端主动向服务端推送一条数据


}
