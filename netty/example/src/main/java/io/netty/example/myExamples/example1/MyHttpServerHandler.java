package io.netty.example.myExamples.example1;

import com.sun.jndi.toolkit.url.Uri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.URI;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 13:50
 */
public class MyHttpServerHandler extends SimpleChannelInboundHandler<HttpObject> {
    // 收到客户单的消息后回调
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        // todo 浏览器发送过来的请求类型: DefaultHttpRequest
        // todo 浏览器发送过来的请求类型: LastHttpContent$

        System.out.println("remote Address: "+ctx.channel().remoteAddress());
        System.out.println("remote Address: "+ctx.channel().id());

        if (msg instanceof HttpRequest) {
            // 可以在这里进行请求的路由
            HttpRequest request = (HttpRequest) msg;
            URI uri = new URI(request.uri());
            System.out.println("请求的路径是: "+uri.getPath());

            // netty中极为重要的组件, bytebuf
            ByteBuf byteBuf = Unpooled.copiedBuffer("hello world", CharsetUtil.UTF_8);

            // 构建响应给客户端的对象
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
            // 设置响应头
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, byteBuf.readableBytes());
            ctx.channel().writeAndFlush(response);
        }
    }




    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelRegistered");
        ctx.fireChannelRegistered();
    }


    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelUnregistered");
        ctx.fireChannelUnregistered();
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelActive");
        ctx.fireChannelActive();
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channelInactive");
        ctx.fireChannelInactive();
    }




    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channnel Read Complete");
        ctx.fireChannelReadComplete();
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        System.out.println("channnel Read userEventTriggered");
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channnel Read channelWritabilityChanged");
        ctx.fireChannelWritabilityChanged();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        System.out.println("exceptionCaught");
        ctx.fireExceptionCaught(cause);
    }


}
