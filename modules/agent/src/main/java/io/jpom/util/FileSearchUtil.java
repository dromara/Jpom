package io.jpom.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.CharsetUtil;
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
 * @author bwcx_jzy
 * @since 2022/5/15
 */
public class FileSearchUtil {

    public static void searchList(File file, Charset charset,
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
        searchList(strings, searchKey, cacheBeforeCount, afterCount, consumer);

    }

    private static void searchList(Collection<Tuple> strings, String searchKey, int cacheBeforeCount, int afterCount, Consumer<Tuple> consumer) {
        AtomicInteger hitIndex = new AtomicInteger(0);
        LimitQueue<Tuple> beforeQueue = new LimitQueue<>(cacheBeforeCount);
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
            if (cacheBeforeCount > 0) {
                //
                beforeQueue.offerFirst(tuple);
            }
        });
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
