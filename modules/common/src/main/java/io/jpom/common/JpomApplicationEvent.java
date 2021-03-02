package io.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.common.spring.event.ApplicationEventClient;
import cn.jiangzeyin.common.spring.event.ApplicationEventLoad;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.JsonFileUtil;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;
import java.io.FileNotFoundException;
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
            File dataDir = FileUtil.file(ConfigBean.getInstance().getDataPath());
            List<File> files = FileUtil.loopFiles(dataDir, 1, pathname -> pathname.getName().startsWith("pid."));
            files.forEach(FileUtil::del);
            DefaultSystemLog.getLog().debug("clear old pid file success");
            try {
                this.lockFile();
            } catch (IOException e) {
                DefaultSystemLog.getLog().error("lockFile", e);
            }
            DefaultSystemLog.getLog().debug("lock pid file success");
            // 写入Jpom 信息
            JpomManifest jpomManifest = JpomManifest.getInstance();
            //  写入全局信息
            File appJpomFile = ConfigBean.getInstance().getApplicationJpomInfo(JpomApplication.getAppType());
            FileUtil.writeString(jpomManifest.toString(), appJpomFile, CharsetUtil.CHARSET_UTF_8);
            // 检查更新文件
            checkUpdate();
            //
            if (ApplicationEventLoad.class.isAssignableFrom(JpomApplication.getAppClass())) {
                ApplicationEventLoad eventLoad = (ApplicationEventLoad) SpringUtil.getBean(JpomApplication.getAppClass());
                eventLoad.applicationLoad();
            }
            Console.log("Jpom Successful start preparation. start loading module");
        } else if (event instanceof ContextClosedEvent) {
            // 应用关闭
            this.unLockFile();
            //
            FileUtil.del(ConfigBean.getInstance().getPidFile());
            //
            File appJpomFile = ConfigBean.getInstance().getApplicationJpomInfo(JpomApplication.getAppType());
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
            } catch (OverlappingFileLockException | IOException e) {
                DefaultSystemLog.getLog().warn("获取进程文件锁失败：" + e.getMessage());
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
            DefaultSystemLog.getLog().error(StrUtil.format("Jpom创建数据目录失败,目录位置：{},请检查当前用户是否有此目录权限或修改配置文件：{}中的jpom.path为可创建目录的路径", path, extConfigPath), e);
            System.exit(-1);
        }
        FileUtil.del(file);
    //        Console.log("", path);
        Console.log("Jpom[{}] 当前数据路径：{} 外部配置文件路径：{}", JpomManifest.getInstance().getVersion(), path, extConfigPath);
    }

    private static void checkUpdate() {
        File runFile = JpomManifest.getRunPath().getParentFile();
        String upgrade = FileUtil.file(runFile, ConfigBean.UPGRADE).getAbsolutePath();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) JsonFileUtil.readJson(upgrade);
        } catch (FileNotFoundException ignored) {
        }
        if (jsonObject == null) {
            return;
        }
        String beforeJar = jsonObject.getString("beforeJar");
        if (StrUtil.isEmpty(beforeJar)) {
            return;
        }
        File beforeJarFile = FileUtil.file(runFile, beforeJar);
        if (beforeJarFile.exists()) {
            File oldJars = FileUtil.file(runFile, "oldJars");
            FileUtil.mkdir(oldJars);
            FileUtil.move(beforeJarFile, oldJars, true);
            DefaultSystemLog.getLog().info("备份旧程序包：" + beforeJar);
        }
        // windows 备份日志
        //        if (SystemUtil.getOsInfo().isWindows()) {
        //            boolean logBack = jsonObject.getBooleanValue("logBack");
        //            String oldLogName = jsonObject.getString("oldLogName");
        //            if (logBack && StrUtil.isNotEmpty(oldLogName)) {
        //                File scriptFile = JpomManifest.getScriptFile();
        //                File oldLog = FileUtil.file(scriptFile.getParentFile(), oldLogName);
        //                if (oldLog.exists()) {
        //                    File logBackDir = FileUtil.file(scriptFile.getParentFile(), "log");
        //                    FileUtil.move(oldLog, logBackDir, true);
        //                }
        //            }
        //        }
    }

}
