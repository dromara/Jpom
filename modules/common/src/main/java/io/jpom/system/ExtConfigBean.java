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
package io.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.charset.Charset;

/**
 * 外部资源配置
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Configuration
@Getter
public class ExtConfigBean {

    public static final String FILE_NAME = "extConfig.yml";

    private static Resource resource;
    /**
     * 请求日志
     */
    @Value("${consoleLog.reqXss:true}")
    private boolean consoleLogReqXss;
    /**
     * 请求响应
     */
    @Value("${consoleLog.reqResponse:true}")
    private boolean consoleLogReqResponse;

    /**
     * 日志文件的编码格式，如果没有指定就自动识别，自动识别可能出现不准确的情况
     */
    @Value("${log.fileCharset:}")
    private String logFileCharsetStr;
    /**
     * 初始读取日志文件行号
     */
    @Value("${log.intiReadLine:10}")
    private int logInitReadLine;
    /**
     * 控制台编码格式
     */
    @Value("${consoleLog.charset:}")
    private String consoleLogCharsetStr;
    /**
     *
     */
    private Charset consoleLogCharset;
    /**
     * 是否开启秒级匹配
     */
    @Value("${system.timerMatchSecond:false}")
    private Boolean timerMatchSecond;
    /**
     * 旧包文件保留个数
     */
    @Value("${system.oldJarsCount:2}")
    private Integer oldJarsCount;

    @Value("${system.remoteVersionUrl:}")
    private String remoteVersionUrl;
    /**
     *
     */
    private Charset logFileCharset;

    public int getLogInitReadLine() {
        return Math.max(logInitReadLine, 10);
    }

    public Charset getLogFileCharset() {
        // 读取配置的编码格式
        if (logFileCharset == null && StrUtil.isNotBlank(logFileCharsetStr)) {
            try {
                logFileCharset = CharsetUtil.charset(logFileCharsetStr);
            } catch (Exception ignored) {
            }
        }
        return logFileCharset;
    }

    public boolean isConsoleLogReqResponse() {
        return consoleLogReqResponse;
    }

    public boolean isConsoleLogReqXss() {
        return consoleLogReqXss;
    }

    private static ExtConfigBean extConfigBean;

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static Resource getResource() {
        if (resource != null) {
            return resource;
        }
        File file = JpomManifest.getRunPath();
        if (file.isFile()) {
            file = file.getParentFile().getParentFile();
            file = FileUtil.file(file, FILE_NAME);
            if (file.exists() && file.isFile()) {
                resource = new FileSystemResource(file);
                return ExtConfigBean.resource;
            }
        }
        resource = new ClassPathResource("/bin/" + FILE_NAME);
        return ExtConfigBean.resource;
    }

    public static File getResourceFile() {
        File file = JpomManifest.getRunPath();
        file = file.getParentFile().getParentFile();
        file = FileUtil.file(file, FILE_NAME);
        return file;
    }

    /**
     * 单例
     *
     * @return this
     */
    public static ExtConfigBean getInstance() {
        if (extConfigBean == null) {
            extConfigBean = SpringUtil.getBean(ExtConfigBean.class);
        }
        return extConfigBean;
    }

    /**
     * 项目运行存储路径
     */
    @Value("${jpom.path}")
    private String path;

    public String getPath() {
        if (StrUtil.isEmpty(path)) {
            if (JpomManifest.getInstance().isDebug()) {
                // 调试模式 为根路径的 jpom文件
                String oldPath = ((SystemUtil.getOsInfo().isMac() ? "~" : "") + "/jpom/" + JpomApplication.getAppType().name() + StrUtil.SLASH).toLowerCase();
                File newFile = FileUtil.file(FileUtil.getUserHomeDir(), "jpom", JpomApplication.getAppType().name().toLowerCase());
                String absolutePath = FileUtil.getAbsolutePath(newFile);
                if (FileUtil.exist(oldPath) && !FileUtil.equals(newFile, FileUtil.file(oldPath))) {
                    FileUtil.move(FileUtil.file(oldPath), newFile, true);
                    Console.log("数据目录位置发生变化：{} => {}", oldPath, absolutePath);
                }
                //Console.log("本地运行存储的数据：{}", absolutePath);
                path = absolutePath;
            } else {
                // 获取当前项目运行路径的父级
                File file = JpomManifest.getRunPath();
                if (!file.exists() && !file.isFile()) {
                    throw new JpomRuntimeException("请配置运行路径属性【jpom.path】");
                }
                File parentFile = file.getParentFile().getParentFile();
                path = FileUtil.getAbsolutePath(parentFile);
            }
        }
        return path;
    }

    public Charset getConsoleLogCharset() {
        if (consoleLogCharset == null) {
            consoleLogCharset = CharsetUtil.parse(consoleLogCharsetStr, CharsetUtil.systemCharset());
        }
        return consoleLogCharset;
    }

    public boolean getTimerMatchSecond() {
        return ObjectUtil.defaultIfNull(timerMatchSecond, false);
    }

    /**
     * 旧包文件保留个数
     *
     * @return 默认 2 个，0 保留所有
     */
    public int getOldJarsCount() {
        return Math.max(oldJarsCount, 0);
    }
}
