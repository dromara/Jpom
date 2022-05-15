import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.FileMode;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import io.jpom.util.LimitQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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
        Collection<Tuple> strings = this.readLastLine(testFile, CharsetUtil.CHARSET_UTF_8, 10);

        int cacheBeforeCount = 1;
        int afterCount = 1;
        String searchKey = ".*(0999996|0999995).*";
        this.searchList(strings, searchKey, cacheBeforeCount, afterCount);
    }

    private void searchList(Collection<Tuple> strings, String searchKey, int cacheBeforeCount, int afterCount) {
        AtomicInteger hitIndex = new AtomicInteger(0);
        LimitQueue<Tuple> beforeQueue = new LimitQueue<>(cacheBeforeCount);
        List<Integer> cacheLineNum = new LinkedList<>();
        strings.forEach(tuple -> {
            String s = tuple.get(1);
            Integer index = tuple.get(0);
            // System.out.println(s);
            if (StrUtil.contains(s, searchKey) || ReUtil.isMatch(searchKey, s)) {
                // 先输出之前的
                for (Tuple before : beforeQueue) {
                    log(cacheLineNum, before);
                }
                log(cacheLineNum, tuple);
                hitIndex.set(index);
            }
            // 是否需要输出后面的内容
            int i = hitIndex.get();
            if (i > 0 && index > i && index <= i + afterCount) {
                log(cacheLineNum, tuple);
            }
            if (cacheBeforeCount > 0) {
                //
                beforeQueue.offerFirst(tuple);
            }
        });
    }

    private void log(List<Integer> cacheLineNum, Tuple tuple) {
        int index = tuple.get(0);
        if (cacheLineNum.contains(index)) {
            return;
        }
        System.out.println(tuple.get(1) + "");
        cacheLineNum.add(index);
    }


    @Test
    public void testLinNumberReadRange() throws IOException {
        int[] calculate = this.calculate(0, 1, true);
        Collection<Tuple> tuples = this.readRangeLine(testFile, CharsetUtil.CHARSET_UTF_8, calculate);
        int cacheBeforeCount = 1;
        int afterCount = 1;
        String searchKey = "abcdef";
        this.searchList(tuples, searchKey, cacheBeforeCount, afterCount);
    }

    public Collection<Tuple> readLastLine(File file, Charset charset, int line) throws IOException {
        BufferedReader reader = FileUtil.getReader(file, charset);
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        LimitQueue<Tuple> limitQueue = new LimitQueue<>(line);
        while (true) {
            String readLine = lineNumberReader.readLine();
            if (readLine == null) {
                break;
            }
            limitQueue.add(new Tuple(lineNumberReader.getLineNumber(), readLine));
            // System.gc();
        }
        return limitQueue;
    }

    public Collection<Tuple> readRangeLine(File file, Charset charset, int[] range) throws IOException {
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
            // System.gc();
        }
        return list;
    }

    public int[] calculate(int head, int tailLine, boolean first) {
        if (head > 0) {
            return first ? new int[]{Math.min(tailLine, head), head} : new int[]{Math.max(head - tailLine, 1), head};
        }
        return first ? new int[]{tailLine, Integer.MAX_VALUE} : new int[]{tailLine};
    }

    @Test
    public void testCalculate() {
        System.out.println(Arrays.toString(this.calculate(0, 3, false)));
        System.out.println(Arrays.toString(this.calculate(0, 3, true)));

        System.out.println(Arrays.toString(this.calculate(2, 3, false)));
        System.out.println(Arrays.toString(this.calculate(2, 3, true)));

        System.out.println(Arrays.toString(this.calculate(20, 3, true)));
        System.out.println(Arrays.toString(this.calculate(20, 3, false)));
    }

}
