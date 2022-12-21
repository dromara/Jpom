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
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.common.Const;
import io.jpom.common.JpomManifest;
import io.jpom.util.FileUtils;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

/**
 * 外部资源配置
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Slf4j
public class ExtConfigBean {


    /**
     *
     */
    private static Charset consoleLogCharset;

    public static void setConsoleLogCharset(Charset consoleLogCharset) {
        ExtConfigBean.consoleLogCharset = consoleLogCharset;
    }

    public static Charset getConsoleLogCharset() {
        return ObjectUtil.defaultIfNull(consoleLogCharset, CharsetUtil.systemCharset());
    }

    /**
     * 项目运行存储路径
     */
    private static String path;

    public static void setPath(String path) {
        ExtConfigBean.path = path;
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static Resource getResource() {
        String property = SpringUtil.getApplicationContext().getEnvironment().getProperty(ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY);
        Resource configResource = Opt.ofBlankAble(property)
            .map(FileSystemResource::new)
            .flatMap((Function<Resource, Opt<Resource>>) resource -> resource.exists() ? Opt.of(resource) : Opt.empty())
            .orElseGet(() -> {
                ClassPathResource classPathResource = new ClassPathResource(Const.FILE_NAME);
                return classPathResource.exists() ? classPathResource : new ClassPathResource("/config_default/" + Const.FILE_NAME);
            });
        Assert.state(configResource.exists(), "均未找到配置文件");
        return configResource;
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream getConfigResourceInputStream(String name) {
        FileUtils.checkSlip(name);
        String property = SpringUtil.getApplicationContext().getEnvironment().getProperty(ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY);
        InputStream inputStream = Opt.ofBlankAble(property)
            .map((Function<String, InputStream>) s -> {
                File file = FileUtil.file(s);
                File configDir = FileUtil.getParent(file, 1);
                file = FileUtil.file(configDir, name);
                if (FileUtil.isFile(file)) {
                    return FileUtil.getInputStream(file);
                }
                return null;
            })
            .orElseGet(() -> {
                String normalize = FileUtil.normalize("/config_default/" + name);
                ClassPathResource classPathResource = new ClassPathResource(ResourceUtils.CLASSPATH_URL_PREFIX + normalize);
                Assert.state(classPathResource.exists(), "配置文件不存在");
                log.debug("外置配置不存在或者未配置：{},使用默认配置", name);
                try {
                    return classPathResource.getInputStream();
                } catch (IOException e) {
                    throw Lombok.sneakyThrow(e);
                }
            });
        Assert.notNull(inputStream, "均未找到配置文件");
        return inputStream;
    }


    public static String getPath() {
        if (StrUtil.isEmpty(path)) {
            if (JpomManifest.getInstance().isDebug()) {
                // 调试模式 为根路径的 jpom文件
                File newFile = FileUtil.file(FileUtil.getUserHomeDir(), "jpom", JpomApplication.getAppType().name().toLowerCase());
                path = FileUtil.getAbsolutePath(newFile);
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
}
