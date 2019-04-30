package cn.keepbx.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.event.ApplicationEventClient;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ExtConfigBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
            //
            checkPath();
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
            //  写入全局信息
            File appJpomFile = ConfigBean.getInstance().getApplicationJpomInfo(BaseJpomApplication.getAppType());
            FileUtil.writeString(jpomManifest.toString(), appJpomFile, CharsetUtil.CHARSET_UTF_8);
        } else if (event instanceof ContextClosedEvent) {
            // 应用关闭
            this.unLockFile();
            //
            FileUtil.del(ConfigBean.getInstance().getPidFile());
            //
            File appJpomFile = ConfigBean.getInstance().getApplicationJpomInfo(BaseJpomApplication.getAppType());
            FileUtil.del(appJpomFile);
        }
    }

    /**
     * 解锁进程文件
     */
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

    /**
     * 锁住进程文件
     *
     * @throws IOException IO
     */
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

    private static void checkPath() {
        String path = ExtConfigBean.getInstance().getPath();
        String extConfigPath = null;
        try {
            extConfigPath = ExtConfigBean.getResource().getURL().toString();
        } catch (IOException ignored) {
        }
        File file = FileUtil.file(path);
        try {
            FileUtil.mkdir(file);
            file = FileUtil.createTempFile("jpom", ".temp", file, true);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(StrUtil.format("Jpom创建数据目录失败,目录位置：{},请检查当前用户是否有此目录权限或修改配置文件：{}中的jpom.path为可创建目录的路径", path, extConfigPath), e);
            System.exit(-1);
        }
        FileUtil.del(file);
        DefaultSystemLog.LOG().info("Jpom外部配置文件路径：" + extConfigPath);
    }

}
