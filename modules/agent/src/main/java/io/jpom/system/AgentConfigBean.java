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
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseAgentController;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 插件端配置
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Configuration
public class AgentConfigBean {
	/**
	 * 白名单文件
	 */
	public static final String WHITELIST_DIRECTORY = "whitelistDirectory.json";
	/**
	 * 项目数据文件
	 */
	public static final String PROJECT = "project.json";

	public static final String TOMCAT = "tomcat.json";
	/**
	 * 项目回收文件
	 */
	public static final String PROJECT_RECOVER = "project_recover.json";
	/**
	 * 证书文件
	 */
	public static final String CERT = "cert.json";
	/**
	 * 脚本管理数据文件
	 */
	public static final String SCRIPT = "script.json";
	/**
	 * 脚本管理执行记录数据文件
	 */
	public static final String SCRIPT_LOG = "script_log.json";

	/**
	 * Server 端的信息
	 */
	public static final String SERVER_ID = "SERVER.json";

	/**
	 * nginx配置信息
	 */
	public static final String NGINX_CONF = "nginx_conf.json";

	/**
	 * jdk列表信息
	 */
	public static final String JDK_CONF = "jdk_conf.json";

    /**
     * 环境变量列表信息
     */
	public static final String WORKSPACE_ENV_VAR = "workspace_env_var.json";


	private static AgentConfigBean agentConfigBean;

	/**
	 * 单利模式
	 *
	 * @return config
	 */
	public static AgentConfigBean getInstance() {
		if (agentConfigBean == null) {
			agentConfigBean = SpringUtil.getBean(AgentConfigBean.class);
		}
		return agentConfigBean;
	}

	/**
	 * 获取当前登录用户的临时文件存储路径，如果没有登录则抛出异常
	 *
	 * @return 文件夹
	 */
	public String getTempPathName() {
		File file = getTempPath();
		return FileUtil.normalize(file.getPath());
	}

	/**
	 * 获取当前登录用户的临时文件存储路径，如果没有登录则抛出异常
	 *
	 * @return file
	 */
	public File getTempPath() {
		File file = ConfigBean.getInstance().getTempPath();
		String userName = BaseAgentController.getNowUserName();
		if (StrUtil.isEmpty(userName)) {
			throw new JpomRuntimeException("没有登录");
		}
		file = FileUtil.file(file, userName);
		FileUtil.mkdir(file);
		return file;
	}
}
