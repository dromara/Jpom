package io.jpom.common;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.ConfigBean;

import java.io.File;
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
	 * 更新日志
	 */
	private String changelog;

	/**
	 * 上次获取时间
	 */
	private Long lastTime;

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
				if (StrUtil.isEmpty(transitUrl)) {
					return null;
				}
				HttpRequest request = HttpUtil.createGet(transitUrl);
				body = request.execute().body();
				//
				remoteVersion = JSONObject.parseObject(body, RemoteVersion.class);
			}
			if (StrUtil.isEmpty(remoteVersion.getTagName())) {
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

	private static void cacheLoadTime(RemoteVersion remoteVersion) {
		remoteVersion = ObjectUtil.defaultIfNull(remoteVersion, new RemoteVersion());
		remoteVersion.setLastTime(SystemClock.now());
		//
		FileUtil.writeUtf8String(remoteVersion.toString(), getFile());
	}

	private static RemoteVersion cacheInfo() {
		if (!FileUtil.isFile(getFile())) {
			return null;
		}
		String string = FileUtil.readUtf8String(getFile());
		RemoteVersion remoteVersion = JSONObject.parseObject(string, RemoteVersion.class);
		// 判断上次获取时间
		Long lastTime = remoteVersion.getLastTime();
		lastTime = ObjectUtil.defaultIfNull(lastTime, 0L);
		long interval = SystemClock.now() - lastTime;
		if (interval >= TimeUnit.HOURS.toMillis(CHECK_INTERVAL)) {
			// 缓存失效
			return null;
		}
		return remoteVersion;
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
