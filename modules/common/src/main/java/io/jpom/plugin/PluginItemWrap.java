package io.jpom.plugin;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;

/**
 * 插件端对象
 *
 * @author bwcx_jzy
 * @since 2021/12/24
 */
public class PluginItemWrap {

	/**
	 * 配置相关
	 */
	private final PluginConfig pluginConfig;

	/**
	 * 插件名
	 */
	private final String name;

	/**
	 * 插件类名
	 */
	private final Class<? extends IPlugin> className;

	/**
	 * 插件对象
	 */
	private volatile IPlugin plugin;

	public PluginItemWrap(Class<? extends IPlugin> className) {
		this.className = className;
		this.pluginConfig = className.getAnnotation(PluginConfig.class);
		if (StrUtil.isNotEmpty(this.pluginConfig.name())) {
			this.name = this.pluginConfig.name();
		} else {
			this.name = this.pluginConfig.plugin().getName();
		}
	}

	public PluginConfig getPluginConfig() {
		return pluginConfig;
	}

	public String getName() {
		return name;
	}

	public Class<? extends IPlugin> getClassName() {
		return className;
	}

	public IPlugin getPlugin() {
		if (plugin == null) {
			synchronized (className) {
				if (plugin == null) {
					//
					boolean nativeObject = this.pluginConfig.nativeObject();
					if (nativeObject) {
						plugin = ReflectUtil.newInstance(className);
					} else {
						plugin = SpringUtil.getBean(className);
					}
				}
			}
		}
		return plugin;
	}
}
