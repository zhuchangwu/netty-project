package io.netty.example.text.ByteBufText;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * @Author: Changwu
 * @Date: 2019/7/20 14:13
 */
public class text01 {
    public static void main(String[] args) {
        t04();
    }
    //
    public void t01(){
        ByteBuf byteBuf = Unpooled.buffer(10);
        for (int i = 0; i < 10; i++) {
            // 写入字节
            byteBuf.writeByte(i);
            // 写入 int
            //byteBuf.writeInt(i);
        }

        // 绝对方式获取
        for (int i = 0; i < byteBuf.capacity(); i++) {
            // 这种方法不会真的改变 readIndex和writeIndex
            System.out.println(byteBuf.getByte(i));
        }
        // 相对方式获取
        for (int i = 0; i < 10; i++) {
            // 自动 增加索引, 会改变 readIndex 和 writeIndex
            //  System.out.println(byteBuf.readByte());
        }

    }

    public static void t02(){
        // Unpooled 表示未 池化的 ByteBuf, 用完了就会销毁, 不涉及到任何的引用计数
        // 另外,一定得制定好编码情况, 对于不同的编码表, 相同的byte 可能会表示不同的 信息
        ByteBuf byteBuf = Unpooled.copiedBuffer("朱hello", Charset.forName("utf-8"));

        if (byteBuf.hasArray()){ // 判断是堆上缓冲还是直接缓冲
            byte[] array = byteBuf.array(); // array就是ＢｙｔｅＢｕｆ的容器
            String s = new String(array, Charset.forName("utf-8"));
            System.out.println(s);
            System.out.println(byteBuf);
            //UnpooledByteBufAllocator$InstrumentedUnpooledUnsafeHeapByteBuf(ridx: 0, widx: 5, cap: 15)
            // HeapByteBuf 说明它是堆上的
            // ridx  0 我们未读
            // widx  5  写入了hello 使用utf-8 每一个字符都是1个字节
            // cap   15  容量15 ,这个长度是可变的,    默认是原有的字节数*3

            System.out.println("---------------------------------------");
            System.out.println(byteBuf.arrayOffset());  // 0 数组的偏移量, 就是第一个自己位于数组的0index位置上
            System.out.println(byteBuf.readerIndex());  // 0  接下来从0开始读
            System.out.println(byteBuf.writerIndex());  // hello 是从0-4 下一个可写的index是 5
            System.out.println(byteBuf.capacity());  // 容量

            System.out.println("---------------------------------------");
            System.out.println(byteBuf.readableBytes());  // 返回可读的字节的数量, 就是中间部分的容量 = writeIndex-readerIndex

            System.out.println("-----------------读取bytebuf的通用方法----------------------");
            for (int i=0;i<byteBuf.readableBytes();i++){
                // 如果         ByteBuf byteBuf = Unpooled.copiedBuffer("he张llo", Charset.forName("utf-8"));
                // 中间添加了中文, 对utf-8 来说  中文三个字节  结果是 he 三个乱码 llo
                System.out.println((char) byteBuf.getByte(i));
            }
            // 因为上面的 getByte() 不会改变readIndex writeIndex  故我们下面接着用

            // 打印第一个字符
            System.out.println(byteBuf.getCharSequence(0,3,Charset.forName("utf-8")));
        }

    }

    // 三种缓冲区
    public static void t03(){
        // 1. heap buffer  底层是通过byte Array 实现的, 也是日常开发中用的最多的一种
        // 2. Direct buffer  位于堆外的直接缓冲区 , 由操作系统实现
        // 3. composite buffer  复合缓冲区, 这是个容器, 用来盛放其他任意多个缓冲区

        // 创建一个空的复合缓存区
        CompositeByteBuf byteBufs = Unpooled.compositeBuffer();
        // 添加若干个缓冲区
        ByteBuf buffer = Unpooled.buffer(10);
        ByteBuf byteBuf = Unpooled.directBuffer(8);
        // 添加
        byteBufs.addComponents(buffer,byteBuf);

        // 删除
        //byteBufs.removeComponent(0);

        // 使用迭代器迭代
       /* Iterator<ByteBuf> iterator = byteBufs.iterator();
        while(iterator.hasNext()){
            System.out.println(iterator.next());
            //
        }*/
        byteBufs.forEach(System.out::println);

        // 总结:
        /**
         *  Heap buffer : 堆缓存区
         *   是最常用的类型 , byteBuf 将数据存储在jvm的堆中, 并且将实际的数据 放入 backing byte Array中
         *    优点: 存在jvm的堆中, 可以快速创建和快速释放, 并且提供了直接访问 内部字节 数组的方法
         *    缺点: 每次读写数据时, 都需要将数据 复制到 直接缓冲区中(临时开辟,多了个临时拷贝数据的过程) 再进行网络传输
         *
         *  Dirct buffer : 直接缓冲区
         *    不支持使用数组的方法访问数据, 所以的先判断一下, hasArray()
         *    在堆之外,由操作系统进行直接分配内存空间, 不会占用jvm的堆空间
         *     优点:  在使用 socket进行数据传递时, 性能非常好, 省去了复制的过程
         *     缺点:  dirct 直接在内存中, 内存空间的分配和释放比堆空间的更复杂,而且速度慢一些
         *
         *  netty 通过内存池来解决这个问题
         *
         *   重点:
         *      对应后端的业务消息的编解码来说, 推荐使用 heapByteBuf
         *      对IO通信线程,在读写缓冲区时,推荐使用 DirctByteBuf  从而实现零拷贝
         *
         *  compesite : 复合缓冲区
         *
         *
         *   jdk的ByteBuffer 和 netty的ByteBuf 的对比
         *
         *   1. netty的 ByteBuf 使用的读写索引分离的策略   readerIndex  writeIndex
         *          一个新初始化的 ByteBuf  readerIndex ==  writeIndex == 0
         *          一个通过其他 ByteBuf 衍生来的 ByteBuf  readerIndex==0  writeIndex== 原ByteBuf的writeIndex
         *
         *   2. 当读索引 == 写索引  如果继续读取, 抛出异常 索引越界异常
         *
         *   3. 对于ByteBuf的任何读写操作,都会单独维护读索引和写索引
         *
         *
         *   4. jdk底层的 ByteBuffer的大小是不可变的, 而netty ByteBuf 是可以自动的扩容
         *
         *
         *   5 . netty的 ByteBuf 处理campacity之外, 还有 maxCampacity ==> 表示的是底层的字节数组能扩容的最大的大小  默认值 Integer.maxValue()
         *
         */
    }


    // netty引用计数的实现机制, 以及自旋锁的使用技巧
    public static void t04(){
        /**
         *
         *  jdk  ByteBuffer的缺点:
         *  1.    final byte[] hb ;   被final 修饰, 长度固定不变,一旦分配好,不能动态扩容, 指向别的引用
         *        而且, 当待存储到字节很大时. 就会出现数组越界异常, 如果想防止异常出现, 就得提前确定大小
         *
         *      如果空间不足了, 只有一个解决方法,  创建一个全新的ByteBuf,将原来的ByteBuf中的数据复制过去, 这一切操作得由开发者手动完成
         *
         *   2. Bytebuf 只有一个指针标志位, 在进行读写交换时, 使用 flip()
         *
         *
         *  netty的 ByteBuf 的优点:
         *
         *    1.  存储字节的数组是动态的,最大值默认是 整形的最大值 , 这里的动态性, 体现在write()方法会在执行时判断buffer的容量,
         *
         *    2.  ByteBuf 读写索引完全分开
         *
         */
    }


    public static void t05(){
        /**
         *  1. 引用计数
         *  ReferenceCounted
         */

       //  Runtime.getRuntime().availableProcessors()  获取可用CPU核数
    }
}
