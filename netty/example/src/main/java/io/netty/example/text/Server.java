package io.netty.example.text;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.example.text.demo.ServerHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;

import java.nio.channels.Selector;

// todo 一般我们使用都是子类, 去看源码就不得不追溯到他们的父类, 很多方法都是它继承的父类的方法, 直接认为就是它的方法也没毛病

/**
 * @Author: Changwu
 * @Date: 2019/6/9 23:17
 */
public class Server {

    public static void main(String[] args) throws InterruptedException {
        // todo  创建两个 eventGroup boss 接受客户端的连接, 底层就是一个死循环, 不断的监听事件 处理事件
        // new NioEventLoopGroup(1); todo 入参1 表示设置boss设置为1个线程, 默认  = 计算机的 核数*2
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // todo  worker处理客户端的请求
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // todo 创建NettyServer的启动辅助类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    // todo 到目前为止，group（）就是把 上面创建的两个 事件循环组一个给了父类（AbstractBootStrap），一个给了自己
                    .group(bossGroup, workerGroup)

                    // todo  在每个netty服务器启动的时候，都会执行这个方法 ，接收了 NioServerSocketChannel.class 去反射;
                    // todo  channel 是它父类的方法
                    // todo  到目前为止仍然是赋值的操作, 把它赋值给 ServerBootstrap的父类 AbstractServerBootstrap
                    .channel(NioServerSocketChannel.class)

                    // todo 为客户端的连接设置相应的配置属性
                    .childOption(ChannelOption.TCP_NODELAY,true)

                    // todo 为每一个新添加进来的 属性信息, 可以理解成是跟业务逻辑有关 信息
                    .childAttr(AttributeKey.newInstance("MyChildAttr"),"MyChildAttValue")

                    // todo 添加handler
                    // .handler(new ServerHandler())

                    // todo 添加自定义的子处理器, 处理workerGroup 的请求事件
                    .childHandler(new MyServerInitializer()); // 添加自己的Initializer

            // sync() 可以当netty一直在这里等待
            // todo 启动!!!  实际上前面的准备工作都是为了Bind()方法准备的    bind()是它父类的方法, 这里有必要sync同步的等待,毕竟是服务端启动的步奏
            ChannelFuture channelFuture = serverBootstrap.bind(8899).sync();

            Channel channel = channelFuture.channel();

            channel.closeFuture().sync(); // todo 确保程序执行完closeFuture后,再往下进行

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
