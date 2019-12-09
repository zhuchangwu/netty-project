package io.netty.example.myExamples.example3;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 16:40
 */
public class MyServerHandler extends SimpleChannelInboundHandler<String> {

    final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        Channel channel = ctx.channel();
        for (Channel channel1 : channelGroup) {
            if (channel != channel) {
                channel.writeAndFlush(channel.remoteAddress() + " 发送消息： " + msg + "\n");
            } else {
                channel.writeAndFlush("[自己] 发送消息： " + msg + "\n");
            }
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        channelGroup.writeAndFlush("[服务器:] " + channel.remoteAddress() + " 加入群聊\n");
        // 维护这个channel
        channelGroup.add(channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        channelGroup.writeAndFlush("[服务端:]" + channel.remoteAddress() + " 上线");
        channelGroup.add(channel);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
       // 无需这样手动移除 （netty自定调用）channelGroup.remove(channel);
        channelGroup.writeAndFlush("[服务端] " + channel.remoteAddress() + " 离开\n");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress()+" 下线 \n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel channel = ctx.channel();
        channelGroup.remove(channel);
        cause.printStackTrace();
        ctx.close();
    }
    /**
     * 1. 当用户上线时， 服务端打印用户上线了
     *                  通知其他用户，这个用户上线了
     * 2. 当用户下线时， 服务端打印用户下线了
     *                   通知其他用户，这个用户下线了
     *
     * 3。 服务端保存所有的在线的channel
     *
     * 4.      chanel 出现异常， 用户下线 都剔除这个channel
     *
     *  5. 用户发送信息，自己看到【自己】：
     *                  其他人看到 【channel】： 消息
     *
     */
}
