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
package io.jpom;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.SystemUtil;
import io.jpom.common.Const;
import io.jpom.common.JpomAppType;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Jpom
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Slf4j
@Configuration
public class JpomApplication implements DisposableBean {
    /**
     * 程序端口
     */
    @Value("${server.port}")
    private int port;
    /**
     * 数据目录缓存大小
     */
    private long dataSizeCache;


    public int getPort() {
        return port;
    }

    private static volatile JpomApplication jpomApplication;

    /**
     * 单利模式
     *
     * @return config
     */
    public static JpomApplication getInstance() {
        if (jpomApplication == null) {
            synchronized (JpomApplication.class) {
                if (jpomApplication == null) {
                    jpomApplication = SpringUtil.getBean(JpomApplication.class);
                }
            }
        }
        return jpomApplication;
    }

    /**
     * 获取项目运行数据存储文件夹路径
     *
     * @return 文件夹路径
     */
    public String getDataPath() {
        String dataPath = FileUtil.normalize(ExtConfigBean.getPath() + StrUtil.SLASH + Const.DATA);
        FileUtil.mkdir(dataPath);
        return dataPath;
    }

    /**
     * 执行脚本
     *
     * @param inputStream 脚本内容
     * @param function    回调分发
     * @param <T>         值类型
     * @return 返回值
     */
    public <T> T execScript(InputStream inputStream, Function<File, T> function) {
        String sshExecTemplate = IoUtil.readUtf8(inputStream);
        return this.execScript(sshExecTemplate, function);
    }

    /**
     * 执行脚本
     *
     * @param context  脚本内容
     * @param function 回调分发
     * @param <T>      值类型
     * @return 返回值
     */
    public <T> T execScript(String context, Function<File, T> function) {
        String dataPath = this.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
        FileUtil.writeString(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        try {
            return function.apply(scriptFile);
        } finally {
            FileUtil.del(scriptFile);
        }
    }

    /**
     * 获取pid文件
     *
     * @return file
     */
    public File getPidFile() {
        return new File(getDataPath(), StrUtil.format("pid.{}.{}",
            JpomApplication.getAppType().name(), JpomManifest.getInstance().getPid()));
    }

    /**
     * 获取当前项目全局 运行信息文件路径
     *
     * @param type 程序类型
     * @return file
     */
    public File getApplicationJpomInfo(Type type) {
        return FileUtil.file(SystemUtil.getUserInfo().getTempDir(), "jpom", type.name());
    }

    /**
     * 获取 agent 端自动生成的授权文件路径
     *
     * @param dataPath 指定数据路径
     * @return file
     */
    public String getAgentAutoAuthorizeFile(String dataPath) {
        return FileUtil.normalize(dataPath + StrUtil.SLASH + Const.AUTHORIZE);
    }

    /**
     * 获取临时文件存储路径
     *
     * @return file
     */
    public File getTempPath() {
        File file = new File(this.getDataPath());
        file = FileUtil.file(file, "temp");
        FileUtil.mkdir(file);
        return file;
    }

    /**
     * 数据目录大小
     *
     * @return byte
     */
    public long dataSize() {
        String dataPath = getDataPath();
        long size = FileUtil.size(FileUtil.file(dataPath));
        dataSizeCache = size;
        return size;
    }

    /**
     * 获取脚本模板路径
     *
     * @return file
     */
    public File getScriptPath() {
        return FileUtil.file(this.getDataPath(), Const.SCRIPT_DIRECTORY);
    }

    public long getDataSizeCache() {
        return dataSizeCache;
    }

    /**
     * 获取当前程序的类型
     *
     * @return Agent 或者 Server
     */
    public static Type getAppType() {
        Map<String, Object> beansWithAnnotation = SpringUtil.getApplicationContext().getBeansWithAnnotation(JpomAppType.class);
        Class<?> jpomAppClass = Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到 Jpom 类型配置"));
        JpomAppType jpomAppType = jpomAppClass.getAnnotation(JpomAppType.class);
        return jpomAppType.value();
    }

    public static Class<?> getAppClass() {
        Map<String, Object> beansWithAnnotation = SpringUtil.getApplicationContext().getBeansWithAnnotation(SpringBootApplication.class);
        return Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到运行的主类"));
    }

    /**
     * 重启自身
     * 分发会延迟2秒执行正式升级 重启命令
     */
    public static void restart() {
        File scriptFile = JpomManifest.getScriptFile();
        ThreadUtil.execute(() -> {
            // Waiting for method caller,For example, the interface response
            ThreadUtil.sleep(2, TimeUnit.SECONDS);
            try {
                String command = CommandUtil.generateCommand(scriptFile, "restart upgrade");
                File parentFile = scriptFile.getParentFile();
                if (SystemUtil.getOsInfo().isWindows()) {
                    //String result = CommandUtil.execSystemCommand(command, scriptFile.getParentFile());
                    //log.debug("windows restart {}", result);
                    CommandUtil.asyncExeLocalCommand(parentFile, "start /b" + command);
                } else {
                    String jpomService = SystemUtil.get("JPOM_SERVICE");
                    if (StrUtil.isEmpty(jpomService)) {
                        CommandUtil.asyncExeLocalCommand(parentFile, command);
                    } else {
                        CommandUtil.asyncExeLocalCommand(parentFile, "systemctl restart " + jpomService);
                    }
                }
            } catch (Exception e) {
                log.error("重启自身异常", e);
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        log.info("Jpom {} disposable", getAppType());
    }
}
