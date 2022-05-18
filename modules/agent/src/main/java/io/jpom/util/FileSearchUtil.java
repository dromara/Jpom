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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 文件搜索工具
 *
 * @author bwcx_jzy
 * @since 2022/5/15
 */
public class FileSearchUtil {

    /**
     * @param file             文件
     * @param charset          编码格式
     * @param searchKey        搜索关键词
     * @param cacheBeforeCount 关键词前多少行
     * @param afterCount       关键词后多少行
     * @param head             搜索文件头多少行
     * @param tailCount        文件后多少行
     * @param first            是否从头开始读取
     * @param consumer         回调
     * @return 结果描述
     * @throws IOException io
     */
    public static String searchList(File file, Charset charset,
                                    String searchKey,
                                    int cacheBeforeCount, int afterCount,
                                    int head, int tailCount,
                                    boolean first, Consumer<Tuple> consumer) throws IOException {

        int[] calculate = FileSearchUtil.calculate(head, tailCount, first);
        Collection<Tuple> strings;
        if (calculate.length == 1) {
            strings = FileSearchUtil.readLastLine(file, charset, calculate[0]);
        } else {
            strings = FileSearchUtil.readRangeLine(file, charset, calculate);
        }
        int showLine = searchList(strings, searchKey, cacheBeforeCount, afterCount, consumer);
        return StrUtil.format("在 {} 行中搜索到并显示 {} 行", CollUtil.size(strings), showLine);
    }

    private static int searchList(Collection<Tuple> strings, String searchKey, int beforeCount, int afterCount, Consumer<Tuple> consumer) {
        AtomicInteger hitIndex = new AtomicInteger(0);
        LimitQueue<Tuple> beforeQueue = new LimitQueue<>(beforeCount);
        List<Integer> cacheLineNum = new LinkedList<>();
        strings.forEach(tuple -> {
            String s = tuple.get(1);
            Integer index = tuple.get(0);
            // System.out.println(s);
            if (StrUtil.isEmpty(searchKey) || StrUtil.containsIgnoreCase(s, searchKey) || ReUtil.isMatch(searchKey, s)) {
                // 先输出之前的
                for (Tuple before : beforeQueue) {
                    checkEchoCache(cacheLineNum, before, consumer);
                }
                checkEchoCache(cacheLineNum, tuple, consumer);
                hitIndex.set(index);
            }
            // 是否需要输出后面的内容
            int i = hitIndex.get();
            if (i > 0 && index > i && index <= i + afterCount) {
                checkEchoCache(cacheLineNum, tuple, consumer);
            }
            if (beforeCount > 0) {
                //
                beforeQueue.offerFirst(tuple);
            }
        });
        return CollUtil.size(cacheLineNum);
    }

    private static void checkEchoCache(List<Integer> cacheLineNum, Tuple tuple, Consumer<Tuple> consumer) {
        int index = tuple.get(0);
        if (cacheLineNum.contains(index)) {
            return;
        }
        consumer.accept(tuple);
        cacheLineNum.add(index);
    }

    public static Collection<Tuple> readLastLine(File file, Charset charset, int line) throws IOException {
        BufferedReader reader = FileUtil.getReader(file, charset);
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        LimitQueue<Tuple> limitQueue = new LimitQueue<>(line);
        while (true) {
            String readLine = lineNumberReader.readLine();
            if (readLine == null) {
                break;
            }
            limitQueue.add(new Tuple(lineNumberReader.getLineNumber(), readLine));
        }
        return limitQueue;
    }

    public static Collection<Tuple> readRangeLine(File file, Charset charset, int[] range) throws IOException {
        BufferedReader reader = FileUtil.getReader(file, charset);
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        List<Tuple> list = new LinkedList<>();
        while (true) {
            String readLine = lineNumberReader.readLine();
            if (readLine == null) {
                break;
            }
            int lineNumber = lineNumberReader.getLineNumber();
            if (lineNumber >= range[0] && lineNumber <= range[1]) {
                list.add(new Tuple(lineNumber, readLine));
            }
            if (lineNumber > range[1]) {
                break;
            }
        }
        return list;
    }

    /**
     * 计算读取文件行数相关
     *
     * @param head     从文件头开始读取
     * @param tailLine 读最后几乎
     * @param first    是否从头开始读取
     * @return int
     */
    public static int[] calculate(int head, int tailLine, boolean first) {
        if (head > 0) {
            return first ? new int[]{Math.min(tailLine, head), head} : new int[]{Math.max(head - tailLine, 1), head};
        }
        return first ? new int[]{tailLine, Integer.MAX_VALUE} : new int[]{tailLine};
    }

}
