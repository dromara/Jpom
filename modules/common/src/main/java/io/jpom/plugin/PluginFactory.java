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

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.JpomManifest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 插件工厂
 *
 * @author bwcx_jzy
 * @since 2019/8/13
 */
@Slf4j
public class PluginFactory implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ContextClosedEvent> {

    //    private static final List<FeatureCallback> FEATURE_CALLBACKS = new ArrayList<>();
    private static final Map<String, List<PluginItemWrap>> PLUGIN_MAP = new ConcurrentHashMap<>();

//    /**
//     * 添加回调事件
//     *
//     * @param featureCallback 回调
//     */
//    public static void addFeatureCallback(FeatureCallback featureCallback) {
//        FEATURE_CALLBACKS.add(featureCallback);
//    }
//
//    public static List<FeatureCallback> getFeatureCallbacks() {
//        return FEATURE_CALLBACKS;
//    }

    /**
     * 获取插件端
     *
     * @param name 插件名
     * @return 插件对象
     */
    public static IPlugin getPlugin(String name) {
        List<PluginItemWrap> pluginItemWraps = PLUGIN_MAP.get(name);
        PluginItemWrap first = CollUtil.getFirst(pluginItemWraps);
        Assert.notNull(first, "对应找到对应到插件：" + name);
        return first.getPlugin();
    }

    /**
     * 判断是否包含某个插件
     *
     * @param name 插件名
     * @return true 包含
     */
    public static boolean contains(String name) {
        return PLUGIN_MAP.containsKey(name);
    }

    /**
     * 插件数量
     *
     * @return 当前加载的插件数量
     */
    public static int size() {
        return PLUGIN_MAP.size();
    }

    /**
     * 正式环境添加依赖
     */
    private static void init() {
        File runPath = JpomManifest.getRunPath().getParentFile();
        File plugin = FileUtil.file(runPath, "plugin");
        if (!plugin.exists() || plugin.isFile()) {
            return;
        }
        // 加载二级插件包
        File[] dirFiles = plugin.listFiles(File::isDirectory);
        if (dirFiles != null) {
            for (File file : dirFiles) {
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
        // 加载一级独立插件端包
        File[] files = plugin.listFiles(pathname -> FileUtil.isFile(pathname) && FileUtil.JAR_FILE_EXT.equalsIgnoreCase(FileUtil.extName(pathname)));
        if (files != null) {
            for (File file : files) {
                addPlugin(file.getName(), file);
            }
        }
    }

    private static void addPlugin(String pluginName, File file) {
        log.info("加载：{} 插件", pluginName);
        ClassLoader contextClassLoader = ClassLoaderUtil.getClassLoader();
        JarClassLoader.loadJar((URLClassLoader) contextClassLoader, file);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        init();
        // 扫描插件 实现
        Set<Class<?>> classes = ClassUtil.scanPackage("io.jpom", IPlugin.class::isAssignableFrom);
        List<PluginItemWrap> pluginItemWraps = classes
                .stream()
                .filter(aClass -> ClassUtil.isNormalClass(aClass) && aClass.isAnnotationPresent(PluginConfig.class))
                .map(aClass -> new PluginItemWrap((Class<? extends IPlugin>) aClass))
                .filter(pluginItemWrap -> {
                    if (StrUtil.isEmpty(pluginItemWrap.getName())) {
                        log.warn("plugin config name error:{}", pluginItemWrap.getClassName());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        //
        Map<String, List<PluginItemWrap>> pluginMap = CollStreamUtil.groupByKey(pluginItemWraps, PluginItemWrap::getName);
        pluginMap.forEach((key, value) -> {
            // 排序
            value.sort((o1, o2) -> Comparator.comparingInt((ToIntFunction<PluginItemWrap>) value1 -> {
                Order order = value1.getClassName().getAnnotation(Order.class);
                if (order == null) {
                    return 0;
                }
                return order.value();
            }).compare(o1, o2));
            PLUGIN_MAP.put(key, value);
        });
        log.debug("load plugin count:{}", pluginMap.keySet().size());
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        Collection<List<PluginItemWrap>> values = PLUGIN_MAP.values();
        for (List<PluginItemWrap> value : values) {
            for (PluginItemWrap pluginItemWrap : value) {
                IPlugin plugin = pluginItemWrap.getPlugin();
                IoUtil.close(plugin);
            }
        }
    }
}
