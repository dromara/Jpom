import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * @author bwcx_jzy
 * @date 2019/8/26
 */
public class TestFileModify {
    public static void main(String[] args) {
        File file = FileUtil.file("D:\\jpom\\server\\data\\mail_config.json");
        WatchMonitor monitor = WatchUtil.create(file, WatchMonitor.ENTRY_MODIFY);
        monitor.setWatcher(new SimpleWatcher() {
            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                System.out.println("刷新邮箱");
            }
        });
        monitor.start();
    }
}
