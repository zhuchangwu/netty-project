package io.netty.example.text;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * @Author: Changwu
 * @Date: 2019/6/10 15:52
 *  todo 批量创建handler
 */

// todo 自定义的初始化器, 初始化一个 SocketChannel 即 客户端的通道, 他被服务端的 BootStrap启动器使用, 用于处理所有的ChildGroup子事件循环组中的事件
public class MyServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline   channelPipeline = ch.pipeline();
        // 添加解码器
       // channelPipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
       // channelPipeline.addLast(new LengthFieldPrepender(4));

        // 针对字符串的编解码
       // channelPipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
       // channelPipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

        // 添加自己的处理器
        channelPipeline.addLast(new MyDeCoderHandler());
        channelPipeline.addLast(new MyEnCoderHandler());
        channelPipeline.addLast(new MyServerHandler());
      /*channelPipeline.addLast(new MyServerHandlerA());
      channelPipeline.addLast(new MyServerHandlerB());
      channelPipeline.addLast(new MyServerHandlerC());
      channelPipeline.addLast(new OutBoundHandlerA());
      channelPipeline.addLast(new OutBoundHandlerB());
      channelPipeline.addLast(new OutBoundHandlerC());*/

      // todo 异常处理的最佳实践, 最pipeline的最后添加异常处理handler
     // channelPipeline.addLast(new myExceptionCaughtHandler());

    }
}
