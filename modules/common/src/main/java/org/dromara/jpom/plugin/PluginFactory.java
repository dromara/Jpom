/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import cn.keepbx.jpom.plugins.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.system.ExtConfigBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import java.io.File;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 插件工厂
 *
 * @author bwcx_jzy
 * @since 2019/8/13
 */
@Slf4j
public class PluginFactory implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ApplicationEvent> {

    //    private static final List<FeatureCallback> FEATURE_CALLBACKS = new ArrayList<>();
    private static final Map<String, List<PluginItemWrap>> PLUGIN_MAP = new SafeConcurrentHashMap<>();

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
        Assert.notNull(first, I18nMessageUtil.get("i18n.plugin_not_found.a6e5") + name);
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
                if (listFiles == null || listFiles.length == 0) {
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
        log.info(I18nMessageUtil.get("i18n.load_plugin.1f64"), pluginName);
        ClassLoader contextClassLoader = ClassLoaderUtil.getClassLoader();
        JarClassLoader.loadJar((URLClassLoader) contextClassLoader, file);
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        //init();
        // 扫描插件 实现
        Set<Class<?>> classes = ClassUtil.scanPackage("org.dromara.jpom", IPlugin.class::isAssignableFrom);
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
    public void onApplicationEvent(ApplicationEvent event) {
//         <ContextClosedEvent>, ApplicationListener<ApplicationReadyEvent>
        if (event instanceof ContextClosedEvent) {
            Collection<List<PluginItemWrap>> values = PLUGIN_MAP.values();
            for (List<PluginItemWrap> value : values) {
                for (PluginItemWrap pluginItemWrap : value) {
                    IPlugin plugin = pluginItemWrap.getPlugin();
                    IoUtil.close(plugin);
                }
            }
        } else if (event instanceof ApplicationReadyEvent) {
            System.setProperty(IPlugin.DATE_PATH_KEY, ExtConfigBean.getPath());
            System.setProperty(IPlugin.JPOM_VERSION_KEY, JpomManifest.getInstance().getVersion());
        }

    }
}
