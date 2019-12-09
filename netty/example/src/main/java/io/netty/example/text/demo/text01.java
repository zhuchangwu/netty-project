package io.netty.example.text.demo;

import io.netty.util.NettyRuntime;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * @Author: Changwu
 * @Date: 2019/6/22 16:57
 */
public class text01 {
    public static void main(String[] args) {
        System.out.println( Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2)));
    }
}
