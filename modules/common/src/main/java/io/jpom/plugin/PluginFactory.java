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
import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.common.JpomManifest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
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
 * @date 2019/8/13
 */
public class PluginFactory implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final List<FeatureCallback> FEATURE_CALLBACKS = new ArrayList<>();
	private static final Map<String, List<PluginItem>> PLUGIN_MAP = new ConcurrentHashMap<>();

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
	 * 获取插件端
	 *
	 * @param plugin 插件
	 * @return 插件对象
	 */
	public static IPlugin getPlugin(DefaultPlugin plugin) {
		return getPlugin(plugin.getName());
	}

	/**
	 * 获取插件端
	 *
	 * @param name 插件名
	 * @return 插件对象
	 */
	public static IPlugin getPlugin(String name) {
		List<PluginItem> pluginItems = PLUGIN_MAP.get(name);
		PluginItem first = CollUtil.getFirst(pluginItems);
		Assert.notNull(first, "对应找到对应到插件：" + name);
		return first.plugin;
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
		if (JpomManifest.getInstance().isDebug()) {
			return;
		}
		File runPath = JpomManifest.getRunPath().getParentFile();
		File plugin = FileUtil.file(runPath, "plugin");
		if (!plugin.exists() || plugin.isFile()) {
			return;
		}
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
		File[] files = plugin.listFiles(pathname -> FileUtil.isFile(pathname) && FileUtil.JAR_FILE_EXT.equalsIgnoreCase(FileUtil.extName(pathname)));
		if (files != null) {
			for (File file : files) {
				addPlugin(file.getName(), file);
			}
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
		// 扫描插件 实现
		Set<Class<?>> classes = ClassUtil.scanPackage("io.jpom", IPlugin.class::isAssignableFrom);
		List<PluginItem> pluginItems = classes.stream().filter(ClassUtil::isNormalClass).map(aClass -> {
			PluginItem pluginItem = new PluginItem();
			IPlugin plugin = (IPlugin) ReflectUtil.newInstance(aClass);
			pluginItem.plugin = plugin;
			pluginItem.name = plugin.name();
			pluginItem.className = (Class<? extends IPlugin>) aClass;
			return pluginItem;
		}).collect(Collectors.toList());
		//
		Map<String, List<PluginItem>> pluginMap = CollStreamUtil.groupByKey(pluginItems, PluginItem::getName);
		pluginMap.forEach((key, value) -> {
			// 排序
			value.sort((o1, o2) -> Comparator.comparingInt((ToIntFunction<PluginItem>) value1 -> value1.plugin.order()).compare(o1, o2));
			PLUGIN_MAP.put(key, value);
		});

	}

	private static class PluginItem {

		/**
		 * 插件名
		 */
		private String name;

		/**
		 * 插件类名
		 */
		private Class<? extends IPlugin> className;

		/**
		 * 插件对象
		 */
		private IPlugin plugin;

		public String getName() {
			return name;
		}

		public Class<? extends IPlugin> getClassName() {
			return className;
		}

		public IPlugin getPlugin() {
			return plugin;
		}
	}
}
