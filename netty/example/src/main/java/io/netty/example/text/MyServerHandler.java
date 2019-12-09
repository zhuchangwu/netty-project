package io.netty.example.text;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @Author: Changwu
 * @Date: 2019/6/10 15:59
 */

// 这里的泛型是String 类型的, 客户端和服务端之间通过String 进行交互
public class MyServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 打印客户端发送过来的信息
        System.out.println(ctx.channel().remoteAddress()+"  "+msg);
        // todo 给客户端返回数据, 进入查看
        // todo   客户端   1 2 3 4 5 服务端     如果是Channel的writeAndFlush()方法, 输出的内容依次经过 5 4321 到达客户端
        ctx.channel().writeAndFlush("form server: "+msg);

        // todo  在Handler的上下文中也有这个方法
        // todo  客户端   1 2 3 4 5 服务端     如果是ChannelContext的writeAndFlush()方法, 输出的内容从当前pipeline 的 handler的  下一个 开始  到达客户端
        // todo  可以带来更高的效率
        ctx.writeAndFlush(msg);
        /**
         *  todo 结论1: ChannelHandlerContext 与 ChanelHandler 之间的绑定关系是 永远不会改变的, 因此我们对齐进行缓存是没有任何问题的
         *  todo 结论2: 对应它两者的 writeAndFlush()  ChannelContext可以带来更高的效率
         */
    }

    // 服务端出现异常的方法
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
