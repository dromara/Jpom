package io.jpom.model.data;

import io.jpom.model.BaseJsonModel;

import java.util.List;
import java.util.Set;

/**
 * 节点分发白名单
 *
 * @author jiangzeyin
 * @date 2019/4/22
 */
public class ServerWhitelist extends BaseJsonModel {

	/**
	 * 项目的白名单
	 */
	private List<String> outGiving;

	/**
	 * 运行远程下载的 host
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
