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
package io.jpom.plugin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.common.JpomManifest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件工厂
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public class PluginFactory implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final List<FeatureCallback> FEATURE_CALLBACKS = new ArrayList<>();

    /**
     * 添加回调事件
     *
     * @param featureCallback 回调
     */
    public static void addFeatureCallback(FeatureCallback featureCallback) {
        FEATURE_CALLBACKS.add(featureCallback);
    }

    public static List<FeatureCallback> getFeatureCallbacks() {
        return FEATURE_CALLBACKS;
    }

    /**
     * 正式环境添加依赖
     */
    private static void init() {
        if (JpomManifest.getInstance().isDebug()) {
            return;
        }
        File runPath = JpomManifest.getRunPath().getParentFile();
        File plugin = FileUtil.file(runPath, "plugin");
        if (!plugin.exists() || plugin.isFile()) {
            return;
        }
        File[] files = plugin.listFiles(File::isDirectory);
        if (files == null) {
            return;
        }
        for (File file : files) {
            File lib = FileUtil.file(file, "lib");
            if (!lib.exists() || lib.isFile()) {
                continue;
            }
            File[] listFiles = lib.listFiles((dir, name) -> StrUtil.endWith(name, FileUtil.JAR_FILE_EXT, true));
            if (listFiles == null || listFiles.length <= 0) {
                continue;
            }
            addPlugin(file.getName(), lib);
        }
    }

    private static void addPlugin(String pluginName, File file) {
        DefaultSystemLog.getLog().info("加载：{}插件", pluginName);
        ClassLoader contextClassLoader = ClassLoaderUtil.getClassLoader();
        JarClassLoader.loadJar((URLClassLoader) contextClassLoader, file);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        init();
    }
}
