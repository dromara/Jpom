/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.system.SystemUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.util.FileUtils;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Slf4j
public class ExtConfigBean {
    /**
     * 控制台日志编码
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
        Assert.state(configResource.exists(), I18nMessageUtil.get("i18n.config_file_not_found.fc87"));
        return configResource;
    }

    /**
     * 判断是否存在对应的配置资源
     *
     * @param name 名称
     * @return true 存在
     */
    public static boolean existConfigResource(String name) {
        File configResourceFile = getConfigResourceFile(name);
        if (configResourceFile == null) {
            return false;
        }
        return FileUtil.exist(configResourceFile) && FileUtil.isFile(configResourceFile);
    }

    /**
     * 获取对应的配置资源 file 对象
     *
     * @param name 名称
     * @return true 存在
     */
    public static File getConfigResourceFile(String name) {
        FileUtils.checkSlip(name);
        File resourceDir = getConfigResourceDir();
        return Opt.ofBlankAble(resourceDir).map(file -> FileUtil.file(file, name)).orElse(null);
    }

    /**
     * 获取对应的配置资源目录 对象
     */
    public static File getConfigResourceDir() {
        String property = SpringUtil.getApplicationContext().getEnvironment().getProperty(ConfigFileApplicationListener.CONFIG_LOCATION_PROPERTY);
        return Opt.ofBlankAble(property).map(s -> {
            File file = FileUtil.file(s);
            return FileUtil.getParent(file, 1);
        }).orElse(null);
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream getConfigResourceInputStream(String name) {
        InputStream inputStream = tryGetConfigResourceInputStream(name);
        Assert.notNull(inputStream, I18nMessageUtil.get("i18n.config_file_not_found.310e") + name);
        return inputStream;
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream tryGetConfigResourceInputStream(String name) {
        FileUtils.checkSlip(name);
        File configResourceDir = getConfigResourceDir();
        return Opt.ofBlankAble(configResourceDir)
            .map((Function<File, InputStream>) configDir -> {
                File file = FileUtil.file(configDir, name);
                if (FileUtil.isFile(file)) {
                    return FileUtil.getInputStream(file);
                }
                return null;
            })
            .orElseGet(() -> {
                log.debug(I18nMessageUtil.get("i18n.external_config_not_exist_or_not_configured.f24e"), name);
                return tryGetDefaultConfigResourceInputStream(name);
            });
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream tryGetDefaultConfigResourceInputStream(String name) {
        String normalize = FileUtil.normalize("/config_default/" + name);
        ClassPathResource classPathResource = new ClassPathResource(normalize);
        if (!classPathResource.exists()) {
            return null;
        }
        try {
            return classPathResource.getInputStream();
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static InputStream getDefaultConfigResourceInputStream(String name) {
        InputStream inputStream = tryGetDefaultConfigResourceInputStream(name);
        Assert.notNull(inputStream, name + I18nMessageUtil.get("i18n.config_file_not_exist.09dd"));
        return inputStream;
    }

    /**
     * 模糊匹配获取配置文件资源
     *
     * @param matchStr 匹配关键词
     * @return 资源
     */
    public static Resource[] getConfigResources(String matchStr) {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        File configResourceDir = getConfigResourceDir();
        return Opt.ofBlankAble(configResourceDir)
            .map(file -> {
                try {
                    String format = StrUtil.format("{}{}/{}", ResourceUtils.FILE_URL_PREFIX, file.getAbsolutePath(), matchStr);
                    Resource[] resources = pathMatchingResourcePatternResolver.getResources(format);
                    if (ArrayUtil.isEmpty(resources)) {
                        log.warn(I18nMessageUtil.get("i18n.config_file_not_exist_with_message.6a40"), format);
                        return null;
                    }
                    return resources;
                } catch (IOException e) {
                    throw Lombok.sneakyThrow(e);
                }
            })
            .orElse(null);
    }

    /**
     * 模糊匹配获取配置文件资源
     *
     * @param matchStr 匹配关键词
     * @return 资源
     */
    public static Resource[] getDefaultConfigResources(String matchStr) {
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            String format = StrUtil.format("{}/config_default/{}", ResourceUtils.CLASSPATH_URL_PREFIX, matchStr);
            return pathMatchingResourcePatternResolver.getResources(format);
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }
    }


    public static String getPath() {
        if (StrUtil.isEmpty(path)) {
            if (JpomManifest.getInstance().isDebug()) {
                File newFile;
                String jpomDevPath = SystemUtil.get("JPOM_DEV_PATH");
                if (StrUtil.isNotEmpty(jpomDevPath)) {
                    newFile = FileUtil.file(jpomDevPath, JpomApplication.getAppType().name().toLowerCase());
                } else {
                    // 调试模式 为根路径的 jpom文件
                    newFile = FileUtil.file(FileUtil.getUserHomeDir(), "jpom", JpomApplication.getAppType().name().toLowerCase());
                }
                path = FileUtil.getAbsolutePath(newFile);
            } else {
                // 获取当前项目运行路径的父级
                File file = JpomManifest.getRunPath();
                if (!file.exists() && !file.isFile()) {
                    throw new JpomRuntimeException(I18nMessageUtil.get("i18n.configure_run_path_property.356c"));
                }
                File parentFile = file.getParentFile().getParentFile();
                path = FileUtil.getAbsolutePath(parentFile);
            }
        }
        return FileUtil.normalize(path);
    }
}
