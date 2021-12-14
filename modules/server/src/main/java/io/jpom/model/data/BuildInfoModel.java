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

import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.service.h2db.TableName;

/**
 * @author Hotstrip
 * new BuildModel class, for replace old BuildModel
 */
@TableName(value = "BUILD_INFO", name = "构建信息")
public class BuildInfoModel extends BaseWorkspaceModel {

	/**
	 * 仓库 ID
	 */
	private String repositoryId;
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 构建 ID
	 *
	 * @see BuildHistoryLog#getBuildNumberId()
	 */
	private Integer buildId;
	/**
	 * 分组名称
	 */
	@Deprecated
	private String group;
	/**
	 * 分支
	 */
	private String branchName;
	/**
	 * 标签
	 */
	private String branchTagName;
	/**
	 * 构建命令
	 */
	private String script;
	/**
	 * 构建产物目录
	 */
	private String resultDirFile;
	/**
	 * 发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}
	 */
	private Integer releaseMethod;
	/**
	 * 发布方法执行数据关联字段
	 */
	private String releaseMethodDataId;
	/**
	 * 状态
	 */
	private Integer status;
	/**
	 * 触发器token
	 */
	private String triggerToken;
	/**
	 * 额外信息，JSON 字符串格式
	 */
	private String extraData;
	/**
	 * 构建 webhook
	 */
	private String webhook;
	/**
	 * 定时构建表达式
	 */
	private String autoBuildCron;

	public String getAutoBuildCron() {
		return autoBuildCron;
	}

	public void setAutoBuildCron(String autoBuildCron) {
		this.autoBuildCron = autoBuildCron;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getBuildId() {
		return buildId;
	}

	public void setBuildId(Integer buildId) {
		this.buildId = buildId;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getWebhook() {
		return webhook;
	}

	public void setWebhook(String webhook) {
		this.webhook = webhook;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getResultDirFile() {
		return resultDirFile;
	}

	public void setResultDirFile(String resultDirFile) {
		this.resultDirFile = resultDirFile;
	}

	public Integer getReleaseMethod() {
		return releaseMethod;
	}

	public void setReleaseMethod(Integer releaseMethod) {
		this.releaseMethod = releaseMethod;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getTriggerToken() {
		return triggerToken;
	}

	public void setTriggerToken(String triggerToken) {
		this.triggerToken = triggerToken;
	}

	public String getExtraData() {
		return extraData;
	}

	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}

	public String getReleaseMethodDataId() {
		return releaseMethodDataId;
	}

	public void setReleaseMethodDataId(String releaseMethodDataId) {
		this.releaseMethodDataId = releaseMethodDataId;
	}

	public static String getBuildIdStr(int buildId) {
		return String.format("#%s", buildId);
	}

	public String getBranchTagName() {
		return branchTagName;
	}

	public void setBranchTagName(String branchTagName) {
		this.branchTagName = branchTagName;
	}
}
