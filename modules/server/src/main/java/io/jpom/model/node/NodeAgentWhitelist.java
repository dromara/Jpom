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
