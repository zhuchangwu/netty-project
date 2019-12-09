package io.netty.example.text.demo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.print.attribute.HashAttributeSet;

/**
 * @Author: Changwu
 * @Date: 2019/7/14 19:26
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {
    // todo  下面的方法,会在服务端启动阶段,以及新链接接入阶段回调
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.err.println(" my serverHandler channelActive ");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.err.println(" my serverHandler  channelRegistered ");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.err.println(" my serverHandler  handlerAdded ");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.err.println("  my serverHandler handlerRemoved ");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.err.println(" my serverHandler  channelRead");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.err.println(" my serverHandler   channelReadComplete ");
    }
}
