package io.netty.example.text.coderText;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 *  todo 使用这个解码器, 将 Long 转成 字符串
 * @Author: Changwu
 * @Date: 2019/7/21 13:59
 */
// todo 这里的 Long 表示它将会处理Long类型数据
public class MyMessageToMessageDecoder extends MessageToMessageEncoder<Long> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Long msg, List<Object> out) throws Exception {
        out.add(String.valueOf(msg));
    }
}
