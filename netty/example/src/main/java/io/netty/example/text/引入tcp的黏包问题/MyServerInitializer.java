package io.netty.example.text.引入tcp的黏包问题;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

/**
 * @Author: Changwu
 * @Date: 2019/12/9 13:09
 */
public class MyServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        //为了掩饰tcp的拆包, 黏包问题, 我们不添加任何编解码器
        ch.pipeline().addLast(new MyServerHandler());
    }
}
