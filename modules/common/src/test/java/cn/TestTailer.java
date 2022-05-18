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
package cn;

import cn.hutool.core.collection.BoundedPriorityQueue;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.Tailer;
import cn.hutool.core.util.StrUtil;
import io.jpom.util.LimitQueue;
import org.junit.Test;

import java.io.File;
import java.util.Comparator;

public class TestTailer {
    public static void main(String[] args) {

        Tailer tailer = new Tailer(new File("D:\\Idea\\Jpom\\modules\\server\\target\\classes\\log\\request\\request-2019-07-21.0.log"), new LineHandler() {
            @Override
            public void handle(String line) {
                System.out.println(line);
            }
        }, 10);
        tailer.start(true);
        System.out.println("12");
    }

    @Test
    public void testLimitQueue() {
        LimitQueue<String> limitQueue = new LimitQueue<>(5);
        for (int i = 0; i < 20; i++) {
            limitQueue.offer(i + "");
            System.out.println((i + 1) + "  " + CollUtil.join(limitQueue, StrUtil.SPACE));
        }
        //
        System.out.println("-------");
        BoundedPriorityQueue<Integer> boundedPriorityQueue = new BoundedPriorityQueue<>(5, Comparator.reverseOrder());
        for (int i = 0; i < 20; i++) {
            boundedPriorityQueue.offer(i);
            System.out.println((i + 1) + "  " + CollUtil.join(boundedPriorityQueue, StrUtil.SPACE));
        }
    }

    @Test
    public void testLine() {

    }

}
