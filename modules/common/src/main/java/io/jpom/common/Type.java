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
package io.jpom.common;

import java.util.function.Function;

/**
 * Jpom 程序类型
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
public enum Type {
	/**
	 * 插件端
	 */
	Agent("io.jpom.JpomAgentApplication", RemoteVersion::getAgentUrl, "KeepBx-Agent-System-JpomAgentApplication"),
	/**
	 * 中心服务端
	 */
	Server("io.jpom.JpomServerApplication", RemoteVersion::getServerUrl, "KeepBx-System-JpomServerApplication"),
	;

	private final Function<RemoteVersion, String> remoteUrl;
	private final String applicationClass;
	private final String tag;

	Type(String applicationClass, Function<RemoteVersion, String> remoteUrl, String tag) {
		this.applicationClass = applicationClass;
		this.remoteUrl = remoteUrl;
		this.tag = tag;
	}

	public String getRemoteUrl(RemoteVersion remoteVersion) {
		return remoteUrl.apply(remoteVersion);
	}

	public String getApplicationClass() {
		return applicationClass;
	}

	public String getTag() {
		return tag;
	}
}
