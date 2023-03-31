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
package org.dromara.jpom.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.cron.IAsyncLoad;
import org.dromara.jpom.cron.ICron;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.JsonFileUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 启动 、关闭监听
 *
 * @author jiangzeyin
 * @since 2019/4/7
 */
@Slf4j
@Configuration
public class JpomApplicationEvent implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

    private final JpomApplication configBean;

    private static int oldJarsCount = 2;

    public static void setOldJarsCount(int oldJarsCount) {
        JpomApplicationEvent.oldJarsCount = oldJarsCount;
    }

    public JpomApplicationEvent(JpomApplication configBean) {
        this.configBean = configBean;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // 启动最后的预加载
        if (event instanceof ApplicationReadyEvent) {

        } else if (event instanceof ContextClosedEvent) {
            //
        }
    }


    private void checkPath() {
        String path = ExtConfigBean.getPath();
        String extConfigPath;
        try {
            extConfigPath = ExtConfigBean.getResource().getURL().toString();
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
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

    private void install() {
        String installId;
        File file = FileUtil.file(configBean.getDataPath(), Const.INSTALL);
        if (file.exists()) {
            JSONObject jsonObject;
            try {
                jsonObject = JsonFileUtil.readJson(file);
            } catch (FileNotFoundException e) {
                throw Lombok.sneakyThrow(e);
            }
            installId = jsonObject.getString("installId");
            Assert.hasText(installId, "数据错误,安装 ID 不存在");
            log.info("本机安装 ID 为：{}", installId);
        } else {
            JSONObject jsonObject = new JSONObject();
            installId = IdUtil.fastSimpleUUID();
            jsonObject.put("installId", installId);
            jsonObject.put("installTime", DateTime.now().toString());
            jsonObject.put("desc", "请勿删除此文件,删除后关联 id 将失效");
            JsonFileUtil.saveJson(file.getAbsolutePath(), jsonObject);
            log.info("安装成功,本机安装 ID 为：{}", installId);
        }
        JpomManifest.getInstance().setInstallId(installId);
    }

    /**
     * 检查更新包文件状态
     */
    private void checkUpdate() {
        File runFile = JpomManifest.getRunPath().getParentFile();
        String upgrade = FileUtil.file(runFile, Const.UPGRADE).getAbsolutePath();
        JSONObject jsonObject = null;
        try {
            jsonObject = JsonFileUtil.readJson(upgrade);
        } catch (FileNotFoundException ignored) {
        }
        if (jsonObject != null) {
            String beforeJar = jsonObject.getString("beforeJar");
            String newJar = jsonObject.getString("newJar");
            if (StrUtil.isNotEmpty(beforeJar)) {
                File beforeJarFile = FileUtil.file(runFile, beforeJar);
                if (beforeJarFile.exists()) {
                    if (this.canMvOldJar(jsonObject, runFile)) {
                        File oldJars = JpomManifest.getOldJarsPath();
                        FileUtil.mkdir(oldJars);
                        FileUtil.move(beforeJarFile, oldJars, true);
                        log.info("备份旧程序包：{}", beforeJar);
                    } else {
                        log.debug("备份旧程序包失败：{},因为新程序包不存在：{}", beforeJar, newJar);
                    }
                } else {
                    log.debug("备份旧程序包失败：{},因为旧程序包不存在", beforeJar);
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

    private boolean canMvOldJar(JSONObject jsonObject, File runFile) {
        String newJar = jsonObject.getString("newJar");
        if (StrUtil.isEmpty(newJar)) {
            return false;
        }
        File newJarFile = FileUtil.file(runFile, newJar);
        return FileUtil.exist(newJarFile);
    }

    private void clearOldJar() {
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
        files.forEach(file -> {
            FileUtil.del(file);
            log.debug("删除旧程序包：{}", file.getAbsolutePath());
        });
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


    private void clearTemp() {
        log.debug("Automatically clean up temporary directories");
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 检查目录权限
        this.checkPath();
        this.install();
        // 清空临时目录
        this.clearTemp();
        // 开始加载子模块
        Map<String, ILoadEvent> loadEventMap = applicationContext.getBeansOfType(ILoadEvent.class);
        loadEventMap.values()
                .stream()
                .sorted((o1, o2) -> CompareUtil.compare(o1.getOrder(), o2.getOrder()))
                .forEach(iLoadEvent -> {
                    try {
                        iLoadEvent.afterPropertiesSet(applicationContext);
                    } catch (Exception e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
        // 检查更新文件
        this.checkUpdate();
        // 开始异常加载
        this.statLoad();
        // 提示成功消息
        this.success();
    }

    @Configuration
    public static class SystemEvent implements ILoadEvent {

        @Override
        public int getOrder() {
            return LOWEST_PRECEDENCE;
        }

        @Override
        public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
            CronUtils.upsert("system_monitor", "0 0 0,12 * * ?", this::executeTask);
            CronUtils.upsert("system_cache", "0 0/10 * * * ?", this::refresh);
            // 启动执行一次
            ThreadUtil.execute(() -> {
                try {
                    this.executeTask();
                    this.refresh();
                } catch (Exception e) {
                    log.error("执行系统任务异常", e);
                }
            });
        }

        private void executeTask() {
            Map<String, ISystemTask> taskMap = SpringUtil.getBeansOfType(ISystemTask.class);
            Optional.ofNullable(taskMap).ifPresent(map -> map.values().forEach(ISystemTask::executeTask));
        }

        private void refresh() {
            Map<String, ICacheTask> taskMap = SpringUtil.getBeansOfType(ICacheTask.class);
            Optional.ofNullable(taskMap).ifPresent(map -> map.values().forEach(ICacheTask::refreshCache));
        }
    }
}
