package io.netty.example.text.自定义协议与解决拆包粘包;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import sun.nio.ch.SelectorImpl;

import java.nio.charset.Charset;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 21:24
 */
public class MyClientHandler extends SimpleChannelInboundHandler<PersonProtocol> {
    private int count;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PersonProtocol msg) throws Exception {
        byte[] content = msg.getContent();
        int length = msg.getLength();
        System.out.println("客户端接收到的消息  长度 ..."+length);
        System.out.println("客户端接收到的消息  内容..."+new String(content, Charset.forName("utf-8")));

        System.out.println("收到的消息的数量: "+count);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 一旦建立连接,就向服务端发送数据
        for (int i=0;i<10;i++){
            String message = "send from client";
            byte[] bytes = message.getBytes("utf-8");
            int r = message.length();

            PersonProtocol protocol = new PersonProtocol();
            protocol.setLength(r);
            protocol.setContent(bytes);
            // todo 跟进
            ctx.writeAndFlush(protocol);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        System.out.println("出异常了......");
    }
}
