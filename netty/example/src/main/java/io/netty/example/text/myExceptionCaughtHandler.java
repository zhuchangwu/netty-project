package io.netty.example.text;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @Author: Changwu
 * @Date: 2019/7/19 14:57
 */
public class myExceptionCaughtHandler extends ChannelInboundHandlerAdapter {

    // 最终全部的异常都会来到这里
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        /*
        if (cause instanceof 自定义异常1){

        }else if(cause instanceof  自定义异常2){

        }
        */
        // todo 下面不要往下传播了
       // super.exceptionCaught(ctx, cause);
    }
}
