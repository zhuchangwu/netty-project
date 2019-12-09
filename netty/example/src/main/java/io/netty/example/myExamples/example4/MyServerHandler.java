package io.netty.example.myExamples.example4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.bouncycastle.asn1.eac.CVCertificate;

/**
 * @Author: Changwu
 * @Date: 2019/12/7 18:36
 */
public class MyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            String eventType = null;

            switch (event.state()){
                case ALL_IDLE:
                    eventType = "读写空闲";
                case WRITER_IDLE:
                    eventType = "读空闲";
                case READER_IDLE:
                    eventType = "写空闲";
            }

            System.out.println(ctx.channel().remoteAddress()+ "  "+eventType);
            ctx.channel().close();
        }
    }
}
