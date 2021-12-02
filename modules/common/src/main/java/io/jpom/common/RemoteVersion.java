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
package io.jpom.common;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.system.ConfigBean;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 远程的版本信息
 *
 *
 * <pre>
 * {
 * "tag_name": "v2.6.4",
 * "agentUrl": "",
 * "serverUrl": "",
 * "changelog": ""
 * }
 * </pre>
 *
 * @author bwcx_jzy
 * @since 2021/9/19
 */
public class RemoteVersion {

	public static final String CHECK_VERSION_ID = "check_version";
	/**
	 * 主 url 用于拉取远程版本信息
	 */
	private static final String URL = "https://jpom.io/docs/release-versions.json";
	/**
	 * 检查间隔时间
	 */
	private static final int CHECK_INTERVAL = 24;

	/**
	 * 版本信息
	 */
	private String tagName;
	/**
	 * 插件端下载地址
	 */
	private String agentUrl;
	/**
	 * 服务端下载地址
	 */
	private String serverUrl;
	/**
	 * 更新日志 (远程url)
	 */
	private String changelogUrl;
	/**
	 * 更新日志
	 */
	private String changelog;
	/**
	 * 上次获取时间
	 */
	private Long lastTime;

	/**
	 * 是否有新版本
	 */
	private Boolean upgrade;

	public Boolean getUpgrade() {
		return upgrade;
	}

	public void setUpgrade(Boolean upgrade) {
		this.upgrade = upgrade;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getAgentUrl() {
		return agentUrl;
	}

	public void setAgentUrl(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public String getChangelog() {
		return changelog;
	}

	public void setChangelog(String changelog) {
		this.changelog = changelog;
	}

	public Long getLastTime() {
		return lastTime;
	}

	public void setLastTime(Long lastTime) {
		this.lastTime = lastTime;
	}

	public String getChangelogUrl() {
		return changelogUrl;
	}

	public void setChangelogUrl(String changelogUrl) {
		this.changelogUrl = changelogUrl;
	}

	@Override
	public String toString() {
		return JSONObject.toJSONString(this);
	}

	/**
	 * 获取远程最新版本
	 *
	 * @return 版本信息
	 */
	public static RemoteVersion loadRemoteInfo() {
		String body = StrUtil.EMPTY;
		try {
			// 获取缓存中到信息
			RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
			if (remoteVersion == null) {
				// 远程获取
				String transitUrl = RemoteVersion.loadTransitUrl();
				if (StrUtil.isNotEmpty(transitUrl)) {
					HttpRequest request = HttpUtil.createGet(transitUrl);
					body = request.execute().body();
					//
					remoteVersion = JSONObject.parseObject(body, RemoteVersion.class);
				}
			}
			if (remoteVersion == null || StrUtil.isEmpty(remoteVersion.getTagName())) {
				// 没有版本信息
				return null;
			}
			// 缓存信息
			RemoteVersion.cacheLoadTime(remoteVersion);
			return remoteVersion;
		} catch (Exception e) {
			DefaultSystemLog.getLog().warn("获取远程版本信息失败:{} {}", e.getMessage(), body);
			return null;
		}
	}

	/**
	 * 获取第一层信息，用于中转
	 *
	 * @return 中转URL
	 */
	private static String loadTransitUrl() {
		String body = StrUtil.EMPTY;
		try {
			HttpRequest request = HttpUtil.createGet(URL);
			body = request.execute().body();
			//
			JSONObject jsonObject = JSONObject.parseObject(body);
			return jsonObject.getString("url");
		} catch (Exception e) {
			DefaultSystemLog.getLog().warn("获取远程版本信息失败:{} {}", e.getMessage(), body);
			return null;
		}
	}

	/**
	 * 缓存信息
	 *
	 * @param remoteVersion 远程版本信息
	 */
	private static void cacheLoadTime(RemoteVersion remoteVersion) {
		remoteVersion = ObjectUtil.defaultIfNull(remoteVersion, new RemoteVersion());
		remoteVersion.setLastTime(SystemClock.now());
		// 判断是否可以升级
		JpomManifest instance = JpomManifest.getInstance();
		if (!instance.isDebug()) {
			String version = instance.getVersion();
			String tagName = remoteVersion.getTagName();
			tagName = StrUtil.removePrefixIgnoreCase(tagName, "v");
			remoteVersion.setUpgrade(StrUtil.compareVersion(version, tagName) < 0);
		} else {
			remoteVersion.setUpgrade(true);
		}
		// 检查是否存在下载地址
		Type type = instance.getType();
		String remoteUrl = type.getRemoteUrl(remoteVersion);
		if (StrUtil.isEmpty(remoteUrl)) {
			remoteVersion.setUpgrade(false);
		}
		// 获取 changelog
		String changelogUrl = remoteVersion.getChangelogUrl();
		if (StrUtil.isNotEmpty(changelogUrl)) {
			String body = HttpUtil.createGet(changelogUrl).execute().body();
			remoteVersion.setChangelog(body);
		}
		//
		FileUtil.writeUtf8String(remoteVersion.toString(), getFile());
	}

	/**
	 * 当前缓存中的 远程版本信息
	 *
	 * @return RemoteVersion
	 */
	public static RemoteVersion cacheInfo() {
		if (!FileUtil.isFile(getFile())) {
			return null;
		}
		RemoteVersion remoteVersion = null;
		String fileStr = StrUtil.EMPTY;
		try {
			fileStr = FileUtil.readUtf8String(getFile());
			if (StrUtil.isEmpty(fileStr)) {
				return null;
			}
			remoteVersion = JSONObject.parseObject(fileStr, RemoteVersion.class);
		} catch (Exception e) {
			DefaultSystemLog.getLog().warn("解析远程版本信息失败:{} {}", e.getMessage(), fileStr);
		}
		// 判断上次获取时间
		Long lastTime = remoteVersion == null ? 0 : remoteVersion.getLastTime();
		lastTime = ObjectUtil.defaultIfNull(lastTime, 0L);
		long interval = SystemClock.now() - lastTime;
		return interval >= TimeUnit.HOURS.toMillis(CHECK_INTERVAL) ? null : remoteVersion;
	}

	/**
	 * 下载
	 *
	 * @param savePath 下载文件保存路径
	 * @param type     类型
	 * @return 保存的全路径
	 * @throws IOException 异常
	 */
	public static Tuple download(String savePath, Type type) throws IOException {
		RemoteVersion remoteVersion = loadRemoteInfo();
		Assert.notNull(remoteVersion, "没有可用的新版本升级:-1");
		Assert.state(remoteVersion.getUpgrade() != null && remoteVersion.getUpgrade(), "没有可用的新版本升级");
		// 检查是否存在下载地址
		String remoteUrl = type.getRemoteUrl(remoteVersion);
		Assert.hasText(remoteUrl, "存在新版本,下载地址不可用");
		// 下载
		File downloadFileFromUrl = HttpUtil.downloadFileFromUrl(remoteUrl, savePath);
		// 解析压缩包
		File file = JpomManifest.zipFileFind(FileUtil.getAbsolutePath(downloadFileFromUrl), type, savePath);
		// 检查
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(FileUtil.getAbsolutePath(file), type.getApplicationClass());
		Assert.state(error.getCode() == HttpStatus.HTTP_OK, error.getMsg());
		return error.getData();
	}

	/**
	 * 升级
	 *
	 * @param savePath 下载文件保存路径
	 * @throws IOException 异常
	 */
	public static void upgrade(String savePath) throws IOException {
		Type type = JpomManifest.getInstance().getType();
		// 下载
		Tuple data = download(savePath, type);
		File file = data.get(3);
		// 基础检查
		String path = FileUtil.getAbsolutePath(file);
		String version = data.get(0);
		JpomManifest.releaseJar(path, version);
		//
		JpomApplication.restart();
	}

	/**
	 * 保存的文件
	 *
	 * @return file
	 */
	private static File getFile() {
		return FileUtil.file(ConfigBean.getInstance().getDataPath(), ConfigBean.REMOTE_VERSION);
	}
}
