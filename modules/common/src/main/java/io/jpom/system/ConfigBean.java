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
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.common.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 配置项
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@Configuration
public class ConfigBean {

	/**
	 * 用户名header
	 */
	public static final String JPOM_SERVER_USER_NAME = "Jpom-Server-UserName";

	public static final String JPOM_AGENT_AUTHORIZE = "Jpom-Agent-Authorize";

	public static final String DATA = "data";

	public static final int AUTHORIZE_ERROR = 900;
	/**
	 * 脚本模板存放路径
	 */
	public static final String SCRIPT_DIRECTORY = "script";
	/**
	 * 授权信息
	 */
	public static final String AUTHORIZE = "agent_authorize.json";
	/**
	 *
	 */
	public static final String AUTHORIZE_USER_KEY = "jpom.authorize.agentName";
	/**
	 *
	 */
	public static final String AUTHORIZE_AUTHORIZE_KEY = "jpom.authorize.agentPwd";
	/**
	 * 程序升级信息文件
	 */
	public static final String UPGRADE = "upgrade.json";
	/**
	 * 远程版本信息
	 */
	public static final String REMOTE_VERSION = "remote_version.json";
	/**
	 * Jpom 程序运行的 application 标识
	 */
	@Value("${jpom.applicationTag:}")
	public String applicationTag;
	/**
	 * 程序端口
	 */
	@Value("${server.port}")
	private int port;
	/**
	 * 环境
	 */
	@Value("${" + ConfigFileApplicationListener.ACTIVE_PROFILES_PROPERTY + "}")
	private String active;
	/**
	 * 数据目录缓存大小
	 */
	private long dataSizeCache;

	private volatile static ConfigBean configBean;

	/**
	 * 单利模式
	 *
	 * @return config
	 */
	public static ConfigBean getInstance() {
		if (configBean == null) {
			synchronized (ConfigBean.class) {
				if (configBean == null) {
					configBean = SpringUtil.getBean(ConfigBean.class);
				}
			}
		}
		return configBean;
	}

	public int getPort() {
		return port;
	}

	/**
	 * 获取项目运行数据存储文件夹路径
	 *
	 * @return 文件夹路径
	 */
	public String getDataPath() {
		String dataPath = FileUtil.normalize(ExtConfigBean.getInstance().getPath() + StrUtil.SLASH + DATA);
		FileUtil.mkdir(dataPath);
		return dataPath;
	}

	/**
	 * 获取pid文件
	 *
	 * @return file
	 */
	public File getPidFile() {
		return new File(getDataPath(), StrUtil.format("pid.{}.{}",
				JpomApplication.getAppType().name(), JpomManifest.getInstance().getPid()));
	}

	/**
	 * 获取当前项目全局 运行信息文件路径
	 *
	 * @param type 程序类型
	 * @return file
	 */
	public File getApplicationJpomInfo(Type type) {
		return FileUtil.file(SystemUtil.getUserInfo().getTempDir(), "jpom", type.name());
	}

	/**
	 * 获取 agent 端自动生成的授权文件路径
	 *
	 * @param dataPath 指定数据路径
	 * @return file
	 */
	public String getAgentAutoAuthorizeFile(String dataPath) {
		return FileUtil.normalize(dataPath + StrUtil.SLASH + ConfigBean.AUTHORIZE);
	}

	/**
	 * 是否为 pro 模式运行
	 *
	 * @return pro
	 */
	public boolean isPro() {
		return StrUtil.equals(this.active, "pro");
	}

	public String getActive() {
		return active;
	}

	/**
	 * 获取临时文件存储路径
	 *
	 * @return file
	 */
	public File getTempPath() {
		File file = new File(ConfigBean.getInstance().getDataPath());
		file = FileUtil.file(file, "temp");
		FileUtil.mkdir(file);
		return file;
	}

	/**
	 * 数据目录大小
	 *
	 * @return byte
	 */
	public long dataSize() {
		String dataPath = getDataPath();
		long size = FileUtil.size(FileUtil.file(dataPath));
		dataSizeCache = size;
		return size;
	}

	/**
	 * 获取脚本模板路径
	 *
	 * @return file
	 */
	public File getScriptPath() {
		return FileUtil.file(this.getDataPath(), SCRIPT_DIRECTORY);
	}

	public long getDataSizeCache() {
		return dataSizeCache;
	}
}
