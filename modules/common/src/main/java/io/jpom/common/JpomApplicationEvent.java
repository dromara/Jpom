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
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
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

    private final ConfigBean configBean;

    private static int oldJarsCount = 2;

    public static void setOldJarsCount(int oldJarsCount) {
        JpomApplicationEvent.oldJarsCount = oldJarsCount;
    }

    public JpomApplicationEvent(ConfigBean configBean) {
        this.configBean = configBean;
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

            // 清理旧进程新文件
            File dataDir = FileUtil.file(configBean.getDataPath());
            List<File> files = FileUtil.loopFiles(dataDir, 1, pathname -> pathname.getName().startsWith("pid."));
            files.forEach(FileUtil::del);

            // 写入Jpom 信息 、 写入全局信息
            File appJpomFile = configBean.getApplicationJpomInfo(JpomApplication.getAppType());
            FileUtil.writeString(jpomManifest.toString(), appJpomFile, CharsetUtil.CHARSET_UTF_8);
            // 检查更新文件
            this.checkUpdate();
            //
            this.statLoad();
            this.success();
        } else if (event instanceof ContextClosedEvent) {
            //
            FileUtil.del(configBean.getPidFile());
            //
            File appJpomFile = configBean.getApplicationJpomInfo(JpomApplication.getAppType());
            FileUtil.del(appJpomFile);
        }
    }

    private void checkPath() {
        String path = ExtConfigBean.getPath();
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
            asyncExit(-1);
        }
        FileUtil.del(file);
        log.info("Jpom[{}] Current data path：{} External configuration file path：{}", JpomManifest.getInstance().getVersion(), path, extConfigPath);
    }

    /**
     * 检查更新包文件状态
     */
    private void checkUpdate() {
        File runFile = JpomManifest.getRunPath().getParentFile();
        String upgrade = FileUtil.file(runFile, Const.UPGRADE).getAbsolutePath();
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
        files = CollUtil.sub(files, oldJarsCount, size);
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
        int port = configBean.getPort();
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


    private void clearTemp() {
        File file = configBean.getTempPath();
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

    @Bean
    public MappingJackson2HttpMessageConverter objectMapper() {
        ObjectMapper build = createJackson();
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter(build);
//        messageConverter.setDefaultCharset(CharsetUtil.CHARSET_UTF_8);
        return messageConverter;
    }


    /**
     * jackson 配置
     *
     * @return mapper
     */
    private ObjectMapper createJackson() {
        Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder = Jackson2ObjectMapperBuilder.json();
        jackson2ObjectMapperBuilder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
        ObjectMapper build = jackson2ObjectMapperBuilder.build();
        // 忽略空
        build.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 驼峰转下划线
        //        build.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy());
        // long to String
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        build.registerModule(simpleModule);
        //
        build.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        build.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);

        return build;
    }

    /**
     * 异步退出，避免 springboot 锁 synchronized (this.startupShutdownMonitor)
     *
     * @param code 退出码
     * @see AbstractApplicationContext#refresh()
     * @see AbstractApplicationContext#close()
     */

    public static void asyncExit(int code) {
        ThreadUtil.execute(() -> System.exit(code));
    }
}
