package io.netty.example.myExamples.example5;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 18:52
 */
public class MyWebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // websocket本质上是基于http的,故加入http的编解码器
        pipeline.addLast(new HttpServerCodec());
        // 以块的方式写的处理器
        pipeline.addLast(new ChunkedWriteHandler());
        // 将接收到的数据段聚合成一整个数据块
        pipeline.addLast(new HttpObjectAggregator(8192));

        // 针对websocket的处理器
        // todo  一般发起WebSocket请求的格式是:   ws://localhost:port/ws  下面的ws , 是url中后面的ws
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        pipeline.addLast(new MyWebSocketFrameHandler());

    }
}
