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
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.FileMode;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import io.jpom.util.FileSearchUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.IntStream;

/**
 * @author bwcx_jzy
 * @since 2022/5/14
 */
public class TestBigFileRead {

    private File testFile;
    private Long startTime;

    @Before
    public void before() {
        testFile = FileUtil.file(SystemUtil.getUserInfo().getTempDir(), "jpom", "test-big-file.txt");
        startTime = System.currentTimeMillis();
    }

    @After
    public void after() {
        logMemory();
        long end = System.currentTimeMillis();
        System.out.println(DateUtil.formatBetween(end - startTime, BetweenFormatter.Level.MILLISECOND));
    }

    @Test
    public void testWriter() {

        FileUtil.del(testFile);
        System.out.println(FileUtil.getAbsolutePath(testFile));
        BufferedWriter bufferedWriter = FileUtil.getWriter(testFile, CharsetUtil.CHARSET_UTF_8, true);
        int count = 1000000;
        int len = (count + "").length();
        IntStream.range(0, count).forEach(value -> {
            try {

                bufferedWriter.write(StrUtil.fillBefore((value + 1) + "", '0', len) + " => " + RandomUtil.randomString(2000));
                //新起一行 写数据
                bufferedWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        IoUtil.close(bufferedWriter);
        //
        System.out.println(FileUtil.readableFileSize(testFile));

    }

    private void logMemory() {
        System.out.println(StrUtil.format("最大内存: {} 总内存: {}", FileUtil.readableFileSize(Runtime.getRuntime().maxMemory()), FileUtil.readableFileSize(Runtime.getRuntime().totalMemory())));
        System.out.println(StrUtil.format("空闲内存: {} 耗内存: {}", FileUtil.readableFileSize(Runtime.getRuntime().freeMemory()), FileUtil.readableFileSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
    }

    @Test
    public void testJdkFiles() throws IOException {
        Files.readAllLines(FileUtil.file(testFile).toPath());
    }

    @Test
    public void testHutool() throws IOException {
        FileUtil.readLines(testFile, CharsetUtil.CHARSET_UTF_8);
    }

    @Test
    public void testHutool2() throws IOException {
        FileUtil.readLines(testFile, CharsetUtil.CHARSET_UTF_8, new LineHandler() {
            @Override
            public void handle(String line) {

            }
        });
    }

    @Test
    public void testHutool3() throws IOException {
        RandomAccessFile randomAccessFile = FileUtil.createRandomAccessFile(testFile, FileMode.rw);
        FileUtil.readLines(randomAccessFile, CharsetUtil.CHARSET_UTF_8, new LineHandler() {
            @Override
            public void handle(String line) {
                //  System.out.println(line);
            }
        });
    }

    @Test
    public void testTail() throws IOException {
        FileUtil.tail(testFile, CharsetUtil.CHARSET_UTF_8, new LineHandler() {
            @Override
            public void handle(String line) {
                System.out.println(line);
            }
        });
    }

    @Test
    public void testLinNumber() throws IOException {
        BufferedReader reader = FileUtil.getReader(testFile, CharsetUtil.CHARSET_UTF_8);
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        while (true) {
            String readLine = lineNumberReader.readLine();
            if (readLine == null) {
                System.out.println(lineNumberReader.getLineNumber());
                break;
            }
        }
    }

    @Test
    public void testScanner() throws Exception {

        try (FileInputStream inputStream = new FileInputStream(testFile); Scanner sc = new Scanner(inputStream, CharsetUtil.CHARSET_UTF_8.toString())) {
            //一行一行读取数据
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                //System.out.println(line);
            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        }
    }

    @Test
    public void testInput() {
        try {
            BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(testFile.toPath()));
            BufferedReader in = new BufferedReader(new InputStreamReader(bis, CharsetUtil.CHARSET_UTF_8));//10M缓存

            while (in.ready()) {
                String line = in.readLine();

            }
            IoUtil.close(in);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * cat /private/var/folders/w0/gzmt450d1mq3j96ymfr_6rm40000gn/T/jpom/test-big-file.txt | tail -n 10 | grep -E '.*(0999996|0999995).*' -C1
     *
     * @throws IOException io
     */
    @Test
    public void testLinNumberReadLast() throws IOException {


        int cacheBeforeCount = 1;
        int afterCount = 1;
        String searchKey = ".*(0999996|0999995).*";
//        FileSearchUtil.searchList(strings, searchKey, cacheBeforeCount, afterCount, new Consumer<Tuple>() {
//            @Override
//            public void accept(Tuple objects) {
//                System.out.println(objects.get(1) + "");
//            }
//        });

        FileSearchUtil.searchList(testFile, CharsetUtil.CHARSET_UTF_8, searchKey,
            cacheBeforeCount, afterCount, 0, 10, false,
            objects -> System.out.println(objects.get(1) + ""));
    }


    @Test
    public void testLinNumberReadRange() throws IOException {

        int cacheBeforeCount = 1;
        int afterCount = 1;
        String searchKey = "abcdef";
        FileSearchUtil.searchList(testFile, CharsetUtil.CHARSET_UTF_8, searchKey,
            cacheBeforeCount, afterCount, 0, 10, true,
            objects -> System.out.println(objects.get(1) + ""));
    }

    @Test
    public void testCalculate() {
        System.out.println(Arrays.toString(FileSearchUtil.calculate(0, 3, false)));
        System.out.println(Arrays.toString(FileSearchUtil.calculate(0, 3, true)));

        System.out.println(Arrays.toString(FileSearchUtil.calculate(2, 3, false)));
        System.out.println(Arrays.toString(FileSearchUtil.calculate(100, 100, false)));
        System.out.println(Arrays.toString(FileSearchUtil.calculate(2, 3, true)));

        System.out.println(Arrays.toString(FileSearchUtil.calculate(20, 3, true)));
        System.out.println(Arrays.toString(FileSearchUtil.calculate(20, 3, false)));
    }

}
