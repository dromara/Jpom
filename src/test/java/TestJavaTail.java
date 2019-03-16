import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by jiangzeyin on 2019/3/15.
 */
public class TestJavaTail {
    public static void main(String[] args) throws IOException, InterruptedException {
//        realtimeShowLog(new File("D:\\SystemDocument\\Desktop\\jboot-test.log"));

        WatchMonitor watchMonitor = WatchUtil.create("D:\\SystemDocument\\Desktop\\jboot-test.log", WatchMonitor.ENTRY_DELETE, WatchMonitor.ENTRY_MODIFY, WatchMonitor.OVERFLOW);
        watchMonitor.setWatcher(new SimpleWatcher() {
            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                System.out.println(event.context());
                System.out.println(currentPath);

                System.out.println(event.count());
            }
        });
        watchMonitor.start();

//        File file = new File("D:/ssss/a/tset.log");
//        test();
//        realtimeShowLog(file);
    }

    public static void test() throws IOException {

//        String path = FileDescriptorTest.class.getClassLoader().getResource("").getPath()+"test.txt";
        File file = new File("D:/ssss/a/tset.log");
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);

        FileDescriptor descriptor = fileOutputStream.getFD();
        FileInputStream nfis = new FileInputStream(descriptor);
        String msg = IoUtil.read(nfis, CharsetUtil.CHARSET_UTF_8);
        System.out.println(msg);
        System.out.println("nfis>>>" + nfis.read());
        FileInputStream sfis = new FileInputStream(descriptor);
        System.out.println("sfis>>>" + sfis.read());
        System.out.println("nfis>>>" + nfis.read());
        nfis.close();
        try {
            System.out.println("sfis>>>" + sfis.read());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("nfis执行异常");
        }
        sfis.close();
    }

    public static boolean forceDelete(File file) {
        boolean result = file.delete();
        int tryCount = 0;
        while (!result && tryCount++ < 10) {
            System.gc();    //回收资源
            result = file.delete();
        }
        return result;
    }

    private static long lastTimeFileSize = 0;

    public static void realtimeShowLog(File logFile) throws IOException {
        //指定文件可读可写
        final RandomAccessFile randomFile = new RandomAccessFile(logFile, "r");
        lastTimeFileSize = randomFile.length() - 10;
        //启动一个线程每10秒钟读取新增的日志信息
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        exec.scheduleWithFixedDelay(() -> {
            try {
                System.out.println(lastTimeFileSize);
                //获得变化部分的
                randomFile.seek(lastTimeFileSize);
                String tmp;
                while ((tmp = randomFile.readLine()) != null) {
                    tmp = new String(tmp.getBytes(CharsetUtil.CHARSET_ISO_8859_1));
                    if (StrUtil.isEmpty(tmp)) {
                        continue;
                    }
                    System.out.println(tmp);
                }
                lastTimeFileSize = randomFile.length();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
