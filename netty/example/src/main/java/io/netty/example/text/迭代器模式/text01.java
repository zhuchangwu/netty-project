package io.netty.example.text.迭代器模式;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;

/**
 * @Author: Changwu
 * @Date: 2019/7/24 16:19
 */
public class text01 {
    public static void main(String[] args) {

        ByteBuf byteBuf = Unpooled.wrappedBuffer(new byte[]{1, 2, 3, 4});

        byteBuf.forEachByte(b->{
            System.out.println(b);
            return true;
        });




    }
}
