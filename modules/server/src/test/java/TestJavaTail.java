/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
 * Created by bwcx_jzy on 2019/3/15.
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
