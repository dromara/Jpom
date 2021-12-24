package io.jpom.plugin;

import cn.hutool.core.util.StrUtil;

import java.lang.annotation.*;

/**
 * 插件配置 相关属性注解
 *
 * @author bwcx_jzy
 * @since 2021/12/24
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginConfig {

	/**
	 * 是否为原生对象，原生对象将使用 默认构造方法创建单利对象
	 *
	 * @return 默认 原生对象
	 */
	boolean nativeObject() default true;

	/**
	 * 默认插件、该字段优先级最低
	 *
	 * @return 插件名
	 */
	DefaultPlugin plugin() default DefaultPlugin.NULL;

	/**
	 * 插件名、该字段优先级高于 plugin
	 *
	 * @return 插件名
	 */
	String name() default StrUtil.EMPTY;
}
