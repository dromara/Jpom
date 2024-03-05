/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package cn;

import cn.hutool.core.collection.BoundedPriorityQueue;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.Tailer;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.util.LimitQueue;
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
