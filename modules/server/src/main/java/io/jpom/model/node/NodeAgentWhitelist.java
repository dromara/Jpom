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
package io.jpom.model.node;

import io.jpom.model.BaseJsonModel;

/**
 * 服务端配置 白名单
 *
 * @author bwcx_jzy
 * @since 2022/1/21
 */
public class NodeAgentWhitelist extends BaseJsonModel {

	private String nodeIds;
	private String project;
	private String certificate;
	private String nginx;
	private String allowEditSuffix;
	private String allowRemoteDownloadHost;

	public String getNodeIds() {
		return nodeIds;
	}

	public void setNodeIds(String nodeIds) {
		this.nodeIds = nodeIds;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public String getNginx() {
		return nginx;
	}

	public void setNginx(String nginx) {
		this.nginx = nginx;
	}

	public String getAllowEditSuffix() {
		return allowEditSuffix;
	}

	public void setAllowEditSuffix(String allowEditSuffix) {
		this.allowEditSuffix = allowEditSuffix;
	}

	public String getAllowRemoteDownloadHost() {
		return allowRemoteDownloadHost;
	}

	public void setAllowRemoteDownloadHost(String allowRemoteDownloadHost) {
		this.allowRemoteDownloadHost = allowRemoteDownloadHost;
	}
}
