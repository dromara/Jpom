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
package io.jpom.model.log;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.io.FileUtil;
import io.jpom.build.BaseBuildModule;
import io.jpom.build.BuildUtil;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.service.h2db.TableName;

import java.io.File;

/**
 * 构建历史记录
 *
 * @author bwcx_jzy
 * @date 2019/7/17
 * @see BaseBuildModule
 **/
@TableName("BUILDHISTORYLOG")
public class BuildHistoryLog extends BaseWorkspaceModel {
	/**
	 * 发布方式
	 *
	 * @see BuildReleaseMethod
	 * @see BuildInfoModel#getReleaseMethod()
	 */
	private Integer releaseMethod;
	/**
	 * 发布方法的数据id
	 *
	 * @see BuildInfoModel#getReleaseMethodDataId()
	 */
	private String releaseMethodDataId;
	/**
	 * 分发后的操作
	 * 仅在项目发布类型生效
	 *
	 * @see io.jpom.model.AfterOpt
	 * @see BuildInfoModel#getExtraData()
	 */
	private Integer afterOpt;
	/**
	 * 是否清空旧包发布
	 */
	private Boolean clearOld;
	/**
	 * 构建产物目录
	 */
	private String resultDirFile;
	/**
	 * 发布命令  ssh 才能用上
	 */
	private String releaseCommand;
	/**
	 * 发布到ssh中的目录
	 */
	private String releasePath;
	/**
	 * 关联的构建id
	 *
	 * @see BuildInfoModel#getId()
	 */
	private String buildDataId;
	/**
	 * 构建名称
	 */
	private String buildName;
	/**
	 * 构建编号
	 *
	 * @see BuildInfoModel#getBuildId()
	 */
	private Integer buildNumberId;
	/**
	 * 状态
	 *
	 * @see BuildStatus
	 */
	private Integer status;
	/**
	 * 开始时间
	 */
	private Long startTime;
	/**
	 * 结束时间
	 */
	private Long endTime;
	/**
	 * 是否存在构建产物
	 */
	@PropIgnore
	private Boolean hashFile;
	/**
	 * 是否存在日志
	 */
	@PropIgnore
	private Boolean hasLog;

	public Boolean getHashFile() {
		File file = BuildUtil.getHistoryPackageFile(getBuildDataId(), getBuildNumberId(), getResultDirFile());
		hashFile = FileUtil.exist(file);
		return hashFile;
	}

	public void setHashFile(Boolean hashFile) {
		this.hashFile = hashFile;
	}

	public Boolean getHasLog() {
		File file = BuildUtil.getLogFile(getBuildDataId(), getBuildNumberId());
		hasLog = FileUtil.exist(file);
		return hasLog;
	}

	public void setHasLog(Boolean hasLog) {
		this.hasLog = hasLog;
	}

	public Integer getReleaseMethod() {
		return releaseMethod;
	}

	public void setReleaseMethod(Integer releaseMethod) {
		this.releaseMethod = releaseMethod;
	}

	public String getReleaseMethodDataId() {
		return releaseMethodDataId;
	}

	public void setReleaseMethodDataId(String releaseMethodDataId) {
		this.releaseMethodDataId = releaseMethodDataId;
	}

	public Integer getAfterOpt() {
		return afterOpt;
	}

	public void setAfterOpt(Integer afterOpt) {
		this.afterOpt = afterOpt;
	}

	public Boolean getClearOld() {
		return clearOld;
	}

	public void setClearOld(Boolean clearOld) {
		this.clearOld = clearOld;
	}

	public String getResultDirFile() {
		return resultDirFile;
	}

	public void setResultDirFile(String resultDirFile) {
		this.resultDirFile = resultDirFile;
	}

	public String getReleaseCommand() {
		return releaseCommand;
	}

	public void setReleaseCommand(String releaseCommand) {
		this.releaseCommand = releaseCommand;
	}

	public String getReleasePath() {
		return releasePath;
	}

	public void setReleasePath(String releasePath) {
		this.releasePath = releasePath;
	}


	public String getBuildDataId() {
		return buildDataId;
	}

	public void setBuildDataId(String buildDataId) {
		this.buildDataId = buildDataId;
	}

	public Integer getBuildNumberId() {
		return buildNumberId;
	}

	public void setBuildNumberId(Integer buildNumberId) {
		this.buildNumberId = buildNumberId;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public String getBuildName() {
		return buildName;
	}

	public void setBuildName(String buildName) {
		this.buildName = buildName;
	}
}
