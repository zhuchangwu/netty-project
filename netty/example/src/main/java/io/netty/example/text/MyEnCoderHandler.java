package io.netty.example.text;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

/** 编码器
 *  todo 出站处理器 -- > 有个泛型 --Long
 * @Author: Changwu
 * @Date: 2019/7/21 10:54
 */
public class MyEnCoderHandler extends MessageToByteEncoder<Long> {

    // todo 接受的参数的类型是 Long 类型的, 如果是其他类型的数据过来的, 根本进入不到当期都能编码器
    @Override
    protected void encode(ChannelHandlerContext ctx, Long msg, ByteBuf out) throws Exception {
        System.out.println("MyEnCoderHandler...");
        System.out.println(msg);
        // 将消息写入到ByteBuf
        out.writeLong(msg);
    }
}
