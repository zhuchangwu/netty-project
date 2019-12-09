package io.netty.example.text.coderText;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * @Author: Changwu
 * @Date: 2019/7/21 12:26
 *  todo 我的入栈处理器,  解码器  用于服务端 解码  客户端发送到网络中的数据
 */
public class MyReplayingDecoder extends ReplayingDecoder<Void> {


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        System.out.println("MyReplayingDecoder...");

        // todo 在这里面,不需要我们自己添加判断
        out.add(in.readLong());

    }
}
