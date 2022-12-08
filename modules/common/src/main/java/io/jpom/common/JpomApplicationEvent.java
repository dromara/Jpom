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
package io.jpom.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.cron.IAsyncLoad;
import io.jpom.cron.ICron;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.JsonFileUtil;
import io.jpom.util.JvmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 启动 、关闭监听
 *
 * @author jiangzeyin
 * @since 2019/4/7
 */
@Slf4j
@Configuration
public class JpomApplicationEvent implements ApplicationListener<ApplicationEvent> {

    private final ExtConfigBean extConfigBean;

    public JpomApplicationEvent(ExtConfigBean extConfigBean) {
        this.extConfigBean = extConfigBean;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // 启动最后的预加载
        if (event instanceof ApplicationReadyEvent) {
            this.clearTemp();
            this.checkDuplicateRun();
            //
            this.checkPath();
            JpomManifest jpomManifest = JpomManifest.getInstance();
            ConfigBean instance = ConfigBean.getInstance();
            // 清理旧进程新文件
            File dataDir = FileUtil.file(instance.getDataPath());
            List<File> files = FileUtil.loopFiles(dataDir, 1, pathname -> pathname.getName().startsWith("pid."));
            files.forEach(FileUtil::del);

            // 写入Jpom 信息 、 写入全局信息
            File appJpomFile = instance.getApplicationJpomInfo(JpomApplication.getAppType());
            FileUtil.writeString(jpomManifest.toString(), appJpomFile, CharsetUtil.CHARSET_UTF_8);
            // 检查更新文件
            this.checkUpdate();
            //
            this.reqXssLog();
            this.statLoad();
            this.success();
        } else if (event instanceof ContextClosedEvent) {
            //
            ConfigBean instance = ConfigBean.getInstance();
            FileUtil.del(instance.getPidFile());
            //
            File appJpomFile = instance.getApplicationJpomInfo(JpomApplication.getAppType());
            FileUtil.del(appJpomFile);
        }
    }

    private void checkPath() {
        String path = extConfigBean.getPath();
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
            log.error(StrUtil.format("Jpom Failed to create data directory, directory location：{}," +
                "Please check whether the current user has permission to this directory or modify the configuration file：{} jpom.path in is the path where the directory can be created", path, extConfigPath), e);
            System.exit(-1);
        }
        FileUtil.del(file);
        log.info("Jpom[{}] Current data path：{} External configuration file path：{}", JpomManifest.getInstance().getVersion(), path, extConfigPath);
    }

    /**
     * 检查更新包文件状态
     */
    private void checkUpdate() {
        File runFile = JpomManifest.getRunPath().getParentFile();
        String upgrade = FileUtil.file(runFile, ConfigBean.UPGRADE).getAbsolutePath();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) JsonFileUtil.readJson(upgrade);
        } catch (FileNotFoundException ignored) {
        }
        if (jsonObject != null) {
            String beforeJar = jsonObject.getString("beforeJar");
            if (StrUtil.isNotEmpty(beforeJar)) {
                File beforeJarFile = FileUtil.file(runFile, beforeJar);
                if (beforeJarFile.exists()) {
                    File oldJars = JpomManifest.getOldJarsPath();
                    FileUtil.mkdir(oldJars);
                    FileUtil.move(beforeJarFile, oldJars, true);
                    log.info("备份旧程序包：" + beforeJar);
                }
            }
        }
        clearOldJar();
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

    private static void clearOldJar() {
        File oldJars = JpomManifest.getOldJarsPath();
        List<File> files = FileUtil.loopFiles(oldJars, 1, file -> StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true));
        if (CollUtil.isEmpty(files)) {
            return;
        }
        // 排序
        files.sort((o1, o2) -> FileUtil.lastModifiedTime(o2).compareTo(FileUtil.lastModifiedTime(o1)));
        // 截取
        int size = CollUtil.size(files);
        files = CollUtil.sub(files, ExtConfigBean.getInstance().getOldJarsCount(), size);
        // 删除文件
        files.forEach(FileUtil::del);
    }


    @SuppressWarnings("rawtypes")
    private void statLoad() {
        ThreadUtil.execute(() -> {
            // 加载定时器
            Map<String, ICron> cronMap = SpringUtil.getApplicationContext().getBeansOfType(ICron.class);
            cronMap.forEach((name, iCron) -> {
                int startCron = iCron.startCron();
                if (startCron > 0) {
                    log.debug("{} scheduling has been started:{}", name, startCron);
                }
            });
            Map<String, IAsyncLoad> asyncLoadMap = SpringUtil.getApplicationContext().getBeansOfType(IAsyncLoad.class);
            asyncLoadMap.forEach((name, asyncLoad) -> asyncLoad.startLoad());
            //
        });
    }

    /**
     * 输出启动成功的 日志
     */
    private void success() {
        Type type = JpomManifest.getInstance().getType();
        int port = ConfigBean.getInstance().getPort();
        String localhostStr = NetUtil.getLocalhostStr();
        String url = StrUtil.format("http://{}:{}", localhostStr, port);
        if (type == Type.Server) {
            log.info("{} Successfully started,Can use happily => {} 【The current address is for reference only】", type, url);
        } else if (type == Type.Agent) {
            log.info("{} Successfully started,Please go to the server to configure and use,Current node address => {} 【The current address is for reference only】", type, url);
        }
    }

    /**
     * 判断是否重复运行
     */

    private void checkDuplicateRun() {
        try {
            Class<?> appClass = JpomApplication.getAppClass();
            String pid = String.valueOf(JpomManifest.getInstance().getPid());
            Integer mainClassPid = JvmUtil.findMainClassPid(appClass.getName());
            if (mainClassPid == null || pid.equals(ObjectUtil.toString(mainClassPid))) {
                return;
            }
            log.warn("The Jpom program recommends that only one corresponding program be run on a machine：" + JpomApplication.getAppType() + "  pid:" + mainClassPid);
        } catch (Exception e) {
            log.error("检查异常", e);
        }
    }


    private void reqXssLog() {
        if (!ExtConfigBean.getInstance().isConsoleLogReqXss()) {
            // 不在控制台记录请求日志信息
            DefaultSystemLog.setLogCallback(new DefaultSystemLog.LogCallback() {
                @Override
                public void log(DefaultSystemLog.LogType type, Object... log) {
                    //
                    if (type == DefaultSystemLog.LogType.REQUEST_ERROR) {
                        JpomApplicationEvent.log.info(Arrays.toString(log));
                    }
                }

                @Override
                public void logStart(HttpServletRequest request, String id, String url, HttpMethod httpMethod, String ip, Map<String, String> parameters, Map<String, String> header) {

                }

                @Override
                public void logError(HttpServletResponse response, String id, int status) {

                }

                @Override
                public void logTimeOut(HttpServletResponse response, String id, long time) {

                }
            });
        }
    }

    private void clearTemp() {
        File file = ConfigBean.getInstance().getTempPath();
        /**
         * @author Hotstrip
         * use Hutool's FileUtil.del method just put file as param not file's path
         * or else,  may be return Accessdenied exception
         */
        try {
            FileUtil.del(file);
        } catch (Exception e) {
            // Try again  jzy 2021-07-31
            log.warn("Attempt to delete temporary folder failed, try to handle read-only permission：{}", e.getMessage());
            List<File> files = FileUtil.loopFiles(file);
            long count = files.stream().map(file12 -> file12.setWritable(true)).filter(aBoolean -> aBoolean).count();
            log.warn("Cumulative number of files in temporary folder: {}, number of successful processing：{}", CollUtil.size(files), count);
            try {
                FileUtil.del(file.toPath());
            } catch (Exception e1) {
                e1.addSuppressed(e);
                boolean causedBy = ExceptionUtil.isCausedBy(e1, AccessDeniedException.class);
                if (causedBy) {
                    log.error("清除临时文件失败,请手动清理：" + FileUtil.getAbsolutePath(file), e);
                    return;
                }
                log.error("清除临时文件失败,请检查目录：" + FileUtil.getAbsolutePath(file), e);
            }
        }
    }

}
