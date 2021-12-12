/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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

/**
 * 功能模块
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
public enum ClassFeature {
	/**
	 * 没有
	 */
	NULL(""),
	NODE("节点管理"),
	UPGRADE_NODE_LIST("节点升级"),
	SEARCH_PROJECT("搜索项目"),
	SSH("SSH管理"),
	SSH_FILE("SSH文件管理"),
	SSH_TERMINAL("SSH终端"),
	SSH_TERMINAL_LOG("SSH终端日志"),
	OUTGIVING("分发管理"),
	OUTGIVING_LOG("分发日志"),
	OUTGIVING_CONFIG_WHITELIST("分发白名单配置"),
	MONITOR("项目监控"),
	MONITOR_LOG("监控日志"),
	OPT_MONITOR("操作监控"),
	/**
	 * ssh
	 */
	BUILD("在线构建"),
	BUILD_LOG("构建日志"),
	BUILD_REPOSITORY("仓库信息"),
	USER("用户管理"),
	USER_LOG("操作日志"),
	SYSTEM_EMAIL("邮箱配置"),
	SYSTEM_CACHE("系统缓存"),
	SYSTEM_LOG("系统日志"),
	SYSTEM_UPGRADE("在线升级"),
	SYSTEM_CONFIG("系统配置"),
	SYSTEM_CONFIG_IP("系统配置IP白名单"),
	SYSTEM_BACKUP("数据库备份"),
	SYSTEM_WORKSPACE("工作空间"),

	//******************************************     节点管理功能
	PROJECT("项目管理", ClassFeature.NODE),
	PROJECT_FILE("项目文件管理", ClassFeature.NODE),
	PROJECT_LOG("项目日志", ClassFeature.NODE),
	PROJECT_CONSOLE("项目控制台", ClassFeature.NODE),
	JDK_LIST("JDK管理", ClassFeature.NODE),
	NODE_SCRIPT("脚本模板", ClassFeature.NODE),
	TOMCAT("Tomcat", ClassFeature.NODE),
	TOMCAT_FILE("Tomcat file", ClassFeature.NODE),
	TOMCAT_LOG("Tomcat log", ClassFeature.NODE),

	NGINX("Nginx", ClassFeature.NODE),
	SSL("ssl证书", ClassFeature.NODE),
	NODE_CONFIG_WHITELIST("节点白名单配置", ClassFeature.NODE),
	NODE_CONFIG("节点白名单配置", ClassFeature.NODE),
	NODE_CACHE("节点缓存", ClassFeature.NODE),
	NODE_LOG("节点系统日志", ClassFeature.NODE),
	NODE_UPGRADE("节点在线升级", ClassFeature.NODE),


//	PROJECT_RECOVER("项目回收", ClassFeature.NODE),

	;

	private final String name;

	private ClassFeature parent;

	public String getName() {
		return name;
	}

	public ClassFeature getParent() {
		return parent;
	}

	ClassFeature(String name) {
		this.name = name;
	}

	ClassFeature(String name, ClassFeature parent) {
		this.name = name;
		this.parent = parent;
	}
}
