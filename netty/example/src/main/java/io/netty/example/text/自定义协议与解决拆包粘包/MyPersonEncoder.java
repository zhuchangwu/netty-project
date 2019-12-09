package io.netty.example.text.自定义协议与解决拆包粘包;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 20:49
 */
// 我的编码器
public class MyPersonEncoder extends MessageToByteEncoder<PersonProtocol> {

    // todo write动作会传播到 MyPersonEncoder的write方法, 但是我们没有重写, 于是就执行 父类 MessageToByteEncoder的write, 我们进去看
    @Override
    protected void encode(ChannelHandlerContext ctx, PersonProtocol msg, ByteBuf out) throws Exception {
        System.out.println("MyPersonEncoder....");
        // 消息头  长度
        out.writeInt(msg.getLength());
        // 消息体
        out.writeBytes(msg.getContent());
    }
}
