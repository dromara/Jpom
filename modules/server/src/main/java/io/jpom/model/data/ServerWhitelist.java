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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseJsonModel;

import java.util.List;
import java.util.Set;

/**
 * 节点分发白名单
 *
 * @author jiangzeyin
 * @since 2019/4/22
 */
public class ServerWhitelist extends BaseJsonModel {

	public static final String ID = "OUTGIVING_WHITELIST";

	/**
	 * 不同工作空间的 ID
	 *
	 * @param workspaceId 工作空间ID
	 * @return id
	 */
	public static String workspaceId(String workspaceId) {
		return ServerWhitelist.ID + StrUtil.DASHED + workspaceId;
	}

	/**
	 * 项目的白名单
	 */
	private List<String> outGiving;

	/**
	 * 允许远程下载的 host
	 */
	private Set<String> allowRemoteDownloadHost;

	public List<String> getOutGiving() {
		return outGiving;
	}

	public void setOutGiving(List<String> outGiving) {
		this.outGiving = outGiving;
	}

	public Set<String> getAllowRemoteDownloadHost() {
		return allowRemoteDownloadHost;
	}

	public void setAllowRemoteDownloadHost(Set<String> allowRemoteDownloadHost) {
		this.allowRemoteDownloadHost = allowRemoteDownloadHost;
	}
}
