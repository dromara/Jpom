/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.util;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 定长队列
 *
 * @author jiangzeyin
 * @since 2019/3/16
 */
public class LimitQueue<E> extends ConcurrentLinkedDeque<E> {
    private final int limit;

    public LimitQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean offerFirst(E s) {
        if (full()) {
            // 删除最后一个
            pollLast();
        }
        return super.offerFirst(s);
    }

    @Override
    public boolean offerLast(E s) {
        if (full()) {
            // 删除第一个
            pollFirst();
        }
        return super.offerLast(s);
    }

    public boolean full() {
        return size() > limit;
    }
}
