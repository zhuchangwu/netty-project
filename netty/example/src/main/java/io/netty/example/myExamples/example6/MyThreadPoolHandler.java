package io.netty.example.myExamples.example6;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**k
 * @Author: Changwu
 * @Date: 2019/12/9 9:58
 */
public class MyThreadPoolHandler extends SimpleChannelInboundHandler<String> {

    private static ExecutorService threadPool = Executors.newFixedThreadPool(20);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        threadPool.submit(() -> {
            Object result = searchFromMysql(msg);
            ctx.writeAndFlush(ctx);
        });
    }

    public Object searchFromMysql(String msg) {
        Object obj = null;
        // todo  obj =
        return obj;
    }
}
