/*
 * Copyright 2013 The Netty Project
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

package io.netty.buffer;

import io.netty.util.IllegalReferenceCountException;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static io.netty.util.internal.ObjectUtil.checkPositive;

/**
 * Abstract base class for {@link ByteBuf} implementations that count references.
 */
// todo 他是 ByteBuf 引用计数的抽象的基类
public abstract class AbstractReferenceCountedByteBuf extends AbstractByteBuf {

    // todo  原子的整形字段的更新器
    private static final AtomicIntegerFieldUpdater<AbstractReferenceCountedByteBuf> refCntUpdater =
            AtomicIntegerFieldUpdater.newUpdater(AbstractReferenceCountedByteBuf.class, "refCnt");

    private volatile int refCnt = 1;

    protected AbstractReferenceCountedByteBuf(int maxCapacity) {
        super(maxCapacity);
    }

    @Override
    public int refCnt() {
        return refCnt;
    }

    /**
     * An unsafe operation intended for use by a subclass that sets the reference count of the buffer directly
     */
    protected final void setRefCnt(int refCnt) {
        this.refCnt = refCnt;
    }

    // todo 我们看一下，如何+1
    @Override
    public ByteBuf retain() {
        return retain0(1);
    }

    @Override
    public ByteBuf retain(int increment) {
        // todo checkPositive(increment, "increment")  检查是不是一个正数
        // todo retain0( ）
        return retain0(checkPositive(increment, "increment"));
    }

    // todo 来这里了
    private ByteBuf retain0(int increment) {
        // todo 这是个 自旋锁
        for (;;) {
            // todo 新创建的 bytebuf   ==》this.refCnt==1
            // todo refCnt 被 volatile修饰
            int refCnt = this.refCnt;

            // todo nextCnt是 希望修改的 值
            final int nextCnt = refCnt + increment;

            // Ensure we not resurrect (which means the refCnt was 0) and also that we encountered an overflow.
            if (nextCnt <= increment) { // todo 如果条件成立, 只能说明 refCnt==0
                // todo 这样的目的是: refCnt==0 当前对象应该已经 被回收了, 没有必要去再 +1
                throw new IllegalReferenceCountException(refCnt, increment);
            }
            // todo CAS, 基于硬件的给字段原子性 赋值的
            // todo 如果想保证cas对变量的更新是原子的, 那么就得保证,在所有的情况下, 都是使用这个函数进行cas 操作
            /**
             * this 被修改的对象
             * refCnt 希望的更新值
             * nextCnt  更新的值
             *
             *  意思是,更新时如果是自己期望的值,就成功更新成nextCnt ,
             *         如果refCnt != 自己期望的值   更新失败 从新更新, 把最新的值赋值给 refCnt, 从新尝试更新
             */
            if (refCntUpdater.compareAndSet(this, refCnt, nextCnt)) {
                break;
            }
        }
        return this;
    }

    @Override
    public ByteBuf touch() {
        return this;
    }

    @Override
    public ByteBuf touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        return release0(1);
    }

    @Override
    public boolean release(int decrement) {
        return release0(checkPositive(decrement, "decrement"));
    }

    private boolean release0(int decrement) {
        for (;;) {
            int refCnt = this.refCnt;
            if (refCnt < decrement) {
                throw new IllegalReferenceCountException(refCnt, -decrement);
            }

            if (refCntUpdater.compareAndSet(this, refCnt, refCnt - decrement)) {
                if (refCnt == decrement) {
                    deallocate();
                    return true;
                }
                return false;
            }
        }
    }
    /**
     * Called once {@link #refCnt()} is equals 0.
     */
    protected abstract void deallocate();
}
