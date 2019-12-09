package io.netty.example.text.引入tcp的黏包问题;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.util.UUID;

/**
 * @Author: Changwu
 * @Date: 2019/12/9 13:10
 */
public class MyServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private int i;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // 将客户端发送的信息,存放在byte数组中
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);

        System.out.println("接收到客户端的消息: "+new String (bytes, CharsetUtil.UTF_8));
        System.out.println("这是第 : "+(++this.i)+" 条消息.");

        // 构建响应的 ByteBuf,写回客户端
        ByteBuf responseBuf = Unpooled.copiedBuffer(UUID.randomUUID().toString(),CharsetUtil.UTF_8);
        ctx.writeAndFlush(responseBuf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("出现异常了...");
        cause.printStackTrace();
    }
}
