package io.netty.example.text.自定义协议与解决拆包粘包;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.UUID;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 20:52
 */
public class MyServerHandler extends SimpleChannelInboundHandler<PersonProtocol> {
    private int count;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PersonProtocol msg) throws Exception {
        System.out.println("channelRead0...");
        int length = msg.getLength();
        byte[] content = msg.getContent();
        System.out.println("服务端接收到: "+length);
        System.out.println("服务端接收到: "+new String(content, Charset.forName("utf-8")));
        System.out.println("服务端接收到: 数量 "+count++);

        //////////////向客户端写回//////////////
        String responseMsg = UUID.randomUUID().toString();
        int Reslength1 = responseMsg.length();
        byte[] Resbytes  = responseMsg.getBytes("utf-8");

        PersonProtocol personProtocol = new PersonProtocol();
        personProtocol.setContent(Resbytes);
        personProtocol.setLength(Reslength1);

        ctx.writeAndFlush(personProtocol);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
