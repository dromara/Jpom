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
	Agent(RemoteVersion::getAgentUrl),
	/**
	 * 中心服务端
	 */
	Server(RemoteVersion::getServerUrl),
	;

	private final Function<RemoteVersion, String> remoteUrl;

	Type(Function<RemoteVersion, String> remoteUrl) {
		this.remoteUrl = remoteUrl;
	}

	public String getRemoteUrl(RemoteVersion remoteVersion) {
		return remoteUrl.apply(remoteVersion);
	}
}
