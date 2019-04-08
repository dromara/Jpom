package cn.keepbx.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.event.ApplicationEventClient;
import cn.keepbx.jpom.model.JpomManifest;
import cn.keepbx.jpom.system.ConfigBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.List;

/**
 * 启动 、关闭监听
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class JpomApplicationEvent implements ApplicationEventClient {
    private FileLock lock;
    private FileOutputStream fileOutputStream;
    private FileChannel fileChannel;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // 启动最后的预加载
        if (event instanceof ApplicationReadyEvent) {
            // 清理旧进程新文件
            List<File> files = FileUtil.loopFiles(ConfigBean.getInstance().getDataPath(), pathname -> pathname.getName().startsWith("pid."));
            files.forEach(FileUtil::del);
            try {
                this.lockFile();
            } catch (IOException e) {
                DefaultSystemLog.ERROR().error("lockFile", e);
            }
            // 写入Jpom 信息
            JpomManifest jpomManifest = JpomManifest.getInstance();
            File jpomInfo = ConfigBean.getInstance().getJpomInfo();
            FileUtil.writeString(jpomManifest.toString(), jpomInfo, CharsetUtil.CHARSET_UTF_8);
        } else if (event instanceof ContextClosedEvent) {
            // 应用关闭
            this.unLockFile();
            FileUtil.del(ConfigBean.getInstance().getPidFile());
        }
    }

    private void unLockFile() {
        if (lock != null) {
            try {
                lock.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        IoUtil.close(lock);
        IoUtil.close(fileChannel);
        IoUtil.close(fileOutputStream);
    }

    private void lockFile() throws IOException {
        this.fileOutputStream = new FileOutputStream(ConfigBean.getInstance().getPidFile(), true);
        this.fileChannel = fileOutputStream.getChannel();
        while (true) {
            try {
                lock = fileChannel.lock();
                break;
            } catch (OverlappingFileLockException | IOException ignored) {
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取当前程序进程id
     *
     * @return pid
     */
    public static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        // format: "pid@hostname"
        String name = runtime.getName();
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            return -1;
        }
    }
}
