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
package io.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.service.h2db.TableName;

/**
 * 节点实体
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@TableName(value = "NODE_INFO", name = "节点信息")
public class NodeModel extends BaseWorkspaceModel {

	private String url;
	private String loginName;
	private String loginPwd;
	private String name;

	/**
	 * 节点协议
	 */
	private String protocol;
	/**
	 * 开启状态，如果关闭状态就暂停使用节点
	 */
	private Integer openStatus;
	/**
	 * 节点超时时间
	 */
	private Integer timeOut;
	/**
	 * 绑定的sshId
	 */
	private String sshId;

//	/**
//	 * 节点分组
//	 */
//	private String group;

	/**
	 * 监控周期
	 */
	private Integer cycle;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getCycle() {
		return cycle;
	}

	public void setCycle(Integer cycle) {
		this.cycle = cycle;
	}

	public String getSshId() {
		return sshId;
	}

	public void setSshId(String sshId) {
		this.sshId = sshId;
	}

	public Integer getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(Integer timeOut) {
		this.timeOut = timeOut;
	}

	public Integer getOpenStatus() {
		return openStatus;
	}

	public void setOpenStatus(Integer openStatus) {
		this.openStatus = openStatus;
	}

	public boolean isOpenStatus() {
		return openStatus != null && openStatus == 1;
	}

	public NodeModel() {
	}

	public NodeModel(String id) {
		this.setId(id);
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol.toLowerCase();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getLoginPwd() {
		return loginPwd;
	}

	public void setLoginPwd(String loginPwd) {
		this.loginPwd = loginPwd;
	}

	/**
	 * 获取 授权的信息
	 *
	 * @return sha1
	 */
	public String toAuthorize() {
		return SecureUtil.sha1(loginName + "@" + loginPwd);
	}

	public String getRealUrl(NodeUrl nodeUrl) {
		return StrUtil.format("{}://{}{}", getProtocol(), getUrl(), nodeUrl.getUrl());
	}
}
