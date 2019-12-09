package io.netty.example.myExamples.example2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 提前定义好了, 客户端服务端双方传输的数据的格式是 String
 *
 * @Author: Changwu
 * @Date: 2019/12/7 15:50
 */
public class MyServerSockerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("收到了客户端: " + msg);

        ctx.channel().writeAndFlush("收到你的消息");
    }


    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        // 一般出现了异常直接关闭这个连接就ok
        cause.printStackTrace();
        ctx.channel().close();
    }

}
