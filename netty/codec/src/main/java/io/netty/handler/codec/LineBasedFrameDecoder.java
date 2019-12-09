/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ByteProcessor;

import java.util.List;

/**
 * A decoder that splits the received {@link ByteBuf}s on line endings.
 * <p>
 * Both {@code "\n"} and {@code "\r\n"} are handled.
 * For a more general delimiter-based decoder, see {@link DelimiterBasedFrameDecoder}.
 */
public class LineBasedFrameDecoder extends ByteToMessageDecoder {

    /** Maximum length of a frame we're willing to decode.  */
    private final int maxLength; // todo 它规定了这个解码器能解码的最大长度, 超过这个长度就会丢弃
    /** Whether or not to throw an exception as soon as we exceed maxLength. */
    private final boolean failFast; // todo true 表示当解码时超过了最大长度是否抛出异常
    private final boolean stripDelimiter; // todo 表示截取出来的数据包带不带换行符, true 不带

    /** True if we're discarding input because we're already over maxLength.  */
    private boolean discarding; // todo  如果我们解码的长度超过了 maxLength, 这个 discarding为true
    private int discardedBytes; //  todo 表示当前已经丢弃 了多少字节

    /**
     * Creates a new decoder.
     * @param maxLength  the maximum length of the decoded frame.
     *                   A {@link TooLongFrameException} is thrown if
     *                   the length of the frame exceeds this value.
     */
    public LineBasedFrameDecoder(final int maxLength) {
        this(maxLength, true, false);
    }

    /**
     * Creates a new decoder.
     * @param maxLength  the maximum length of the decoded frame.
     *                   A {@link TooLongFrameException} is thrown if
     *                   the length of the frame exceeds this value.
     * @param stripDelimiter  whether the decoded frame should strip out the
     *                        delimiter or not
     * @param failFast  If <tt>true</tt>, a {@link TooLongFrameException} is
     *                  thrown as soon as the decoder notices the length of the
     *                  frame will exceed <tt>maxFrameLength</tt> regardless of
     *                  whether the entire frame has been read.
     *                  If <tt>false</tt>, a {@link TooLongFrameException} is
     *                  thrown after the entire frame that exceeds
     *                  <tt>maxFrameLength</tt> has been read.
     */
    public LineBasedFrameDecoder(final int maxLength, final boolean stripDelimiter, final boolean failFast) {
        this.maxLength = maxLength;
        this.failFast = failFast;
        this.stripDelimiter = stripDelimiter;
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param   ctx             the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param   buffer          the {@link ByteBuf} from which to read data
     * @return  frame           the {@link ByteBuf} which represent the frame or {@code null} if no frame could
     *                          be created.
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        final int eol = findEndOfLine(buffer); // todo 从累加器中找出换行符
        if (!discarding) { // todo 一开始 默认 discarding == false , 则!discarding 为true 进入判断的代码块

            // todo 非丢弃模式下的找到了换行符
            if (eol >= 0) {
                final ByteBuf frame;
                final int length = eol - buffer.readerIndex(); // todo 计算出,从换行符到可读字节之间的长度, 得到的结果表示 这个大小范围的字节可以进行解码

                // todo 获取到分隔符的长度, 如果是\r 说明分隔符是长度为2
                final int delimLength = buffer.getByte(eol) == '\r'? 2 : 1;

                if (length > maxLength) { // todo 判断当前的要被解码的字段 是否大于规定的能解码的最大长度, 这个maxLength是我们通过构造函数传递进入的
                    buffer.readerIndex(eol + delimLength);
                    // todo 超过最大可以解析的长度后, 它直接对 readIndex动手, readIndex = 将被解码的字节长度 + 分隔符长度, 意识就是丢弃了当前的字节数据
                    fail(ctx, length); // todo 传播异常 ,返回
                    return null;
                }

                // todo 程序来到这里说明,没超出我们指定的范围
                if (stripDelimiter) {
                    frame = buffer.readRetainedSlice(length); //todo 从buffer中,把有效的长度读出, 存入 frame
                    buffer.skipBytes(delimLength); // todo 将读指针往后移动,跳过当前的分隔符的长度
                } else {
                    frame = buffer.readRetainedSlice(length + delimLength);
                }

                return frame;
            } else {
            // todo 非对齐模式下的  未找到  换行符
            // todo 此时很可能是最后一段字节流,
                final int length = buffer.readableBytes();
                if (length > maxLength) {   // todo 如果可解析的长度超过了最大的长度
                    discardedBytes = length;
                    buffer.readerIndex(buffer.writerIndex());
                    discarding = true;  // todo 把这个flag 该成了true, 表示解码器开始进入了丢弃模式
                    if (failFast) {
                        fail(ctx, "over " + discardedBytes);
                    }
                }
                return null;
            }
        } else {
            // todo 丢弃模式,无论是否找到  换行符, 直接丢弃
            if (eol >= 0) { // todo
                final int length = discardedBytes + eol - buffer.readerIndex();
                final int delimLength = buffer.getByte(eol) == '\r'? 2 : 1;
                buffer.readerIndex(eol + delimLength);
                discardedBytes = 0;
                discarding = false;
                if (!failFast) {
                    fail(ctx, length);
                }
            } else {
                discardedBytes += buffer.readableBytes();
                buffer.readerIndex(buffer.writerIndex());
            }
            return null;
        }
    }

    private void fail(final ChannelHandlerContext ctx, int length) {
        fail(ctx, String.valueOf(length));
    }

    private void fail(final ChannelHandlerContext ctx, String length) {
        ctx.fireExceptionCaught(
                new TooLongFrameException(
                        "frame length (" + length + ") exceeds the allowed maximum (" + maxLength + ')'));
    }

    /**
     * Returns the index in the buffer of the end of line found.
     * Returns -1 if no end of line was found in the buffer.
     */
    private static int findEndOfLine(final ByteBuf buffer) {
        int i = buffer.forEachByte(ByteProcessor.FIND_LF);
        if (i > 0 && buffer.getByte(i - 1) == '\r') {
            i--;
        }
        return i;
    }
}
