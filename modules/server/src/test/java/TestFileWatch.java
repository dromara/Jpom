/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * Created by bwcx_jzy on 2018/10/2.
 */

public class TestFileWatch {

    @Test
    public void test() {
        File file = FileUtil.file("Y:\\Z.package");
        WatchMonitor monitor = WatchUtil.createAll(file, new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                Path context = (Path) event.context();
                System.out.println(context);
                System.out.println("创建：" + currentPath);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                Path context = (Path) event.context();
                System.out.println("修改：" + currentPath + " " + event.context() + "  " + event.kind());
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                System.out.println("删除：" + currentPath);
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                System.out.println("超限：" + currentPath);
            }
        });
        monitor.run();
    }

    public static void main(String[] args) {
        File file = new File("D:\\SystemDocument\\Desktop\\top.txt");
        WatchMonitor watchMonitor = WatchUtil.create(file);
        watchMonitor.watch(new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {

            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                System.out.println("onModify" + event);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {

            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                System.out.println("onOverflow");
            }
        });
    }
}
