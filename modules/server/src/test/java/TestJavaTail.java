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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.util.CharsetUtil;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

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

}
