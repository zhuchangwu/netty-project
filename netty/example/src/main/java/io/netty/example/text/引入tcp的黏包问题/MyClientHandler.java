package io.netty.example.text.引入tcp的黏包问题;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

/**
 * @Author: Changwu
 * @Date: 2019/12/9 13:17
 */
public class MyClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private int i;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);

        System.out.println("接收到服务端发送消息 " + new String(bytes, CharsetUtil.UTF_8));
        System.out.println("接收到的信息数量: "+(++this.i));
    }


    // 客户端向服务端发送数据
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        for (int i = 0; i < 10; i++) {
            ByteBuf byteBuf = Unpooled.copiedBuffer("i am client", CharsetUtil.UTF_8);
            ctx.writeAndFlush(byteBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("出现异常了...");
        cause.printStackTrace();
    }
}
