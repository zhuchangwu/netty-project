package io.netty.example.text.自定义协议与解决拆包粘包;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 20:45
 */
// 解码器
public class MyPersonDecoder extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        // 通过readInt 读取到自定义协议的长度
        int length = in.readInt();
        byte[] content = new byte[length];// 创建容器
        in.readBytes(content); // 把读到的 字节 封装进content中

        System.out.println("-----------------------------");
        System.out.println("length== "+length);
        System.out.println("-----------------------------");
        // 构造协议对象
        PersonProtocol personProtocol = new PersonProtocol();
        personProtocol.setContent(content);

        out.add(personProtocol);

    }
}
