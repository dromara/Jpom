package io.jpom.common;

import java.util.function.Function;

/**
 * Jpom 程序类型
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
public enum Type {
	/**
	 * 插件端
	 */
	Agent("io.jpom.JpomAgentApplication", RemoteVersion::getAgentUrl),
	/**
	 * 中心服务端
	 */
	Server("io.jpom.JpomServerApplication", RemoteVersion::getServerUrl),
	;

	private final Function<RemoteVersion, String> remoteUrl;
	private final String applicationClass;

	Type(String applicationClass, Function<RemoteVersion, String> remoteUrl) {
		this.applicationClass = applicationClass;
		this.remoteUrl = remoteUrl;
	}

	public String getRemoteUrl(RemoteVersion remoteVersion) {
		return remoteUrl.apply(remoteVersion);
	}

	public String getApplicationClass() {
		return applicationClass;
	}
}
