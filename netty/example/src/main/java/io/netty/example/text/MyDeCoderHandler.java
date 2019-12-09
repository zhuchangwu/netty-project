package io.netty.example.text;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;

/**
 *  todo 入栈处理器
 * @Author: Changwu
 * @Date: 2019/7/21 10:54
 */
public class MyDeCoderHandler extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        System.out.println("MyDeCoderHandler invoke...");
        System.out.println(in.readableBytes());
        if (in.readableBytes()>=8){
            out.add(in.readLong());
        }
    }
}
