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
package io.jpom.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseModel;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.log.BuildHistoryLog;

/**
 * 构建物基类
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public class BuildExtraModule extends BaseModel {
	/**
	 * 发布方式
	 *
	 * @see BuildReleaseMethod
	 * @see BuildInfoModel#getReleaseMethod()
	 */
	private int releaseMethod;
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
	private int afterOpt;
	/**
	 * 是否清空旧包发布
	 */
	private boolean clearOld;
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
	 * 工作空间 ID
	 */
	private String workspaceId;
	/**
	 * 增量同步
	 */
	private boolean diffSync;

	public boolean isDiffSync() {
		return diffSync;
	}

	public void setDiffSync(boolean diffSync) {
		this.diffSync = diffSync;
	}

	public String getReleasePath() {
		return releasePath;
	}

	public void setReleasePath(String releasePath) {
		this.releasePath = releasePath;
	}

	public String getReleaseCommand() {
		return releaseCommand;
	}

	public void setReleaseCommand(String releaseCommand) {
		this.releaseCommand = releaseCommand;
	}

	public boolean isClearOld() {
		return clearOld;
	}

	public void setClearOld(boolean clearOld) {
		this.clearOld = clearOld;
	}

	public int getReleaseMethod() {
		return releaseMethod;
	}

	public void setReleaseMethod(int releaseMethod) {
		this.releaseMethod = releaseMethod;
	}

	public String getReleaseMethodDataId() {
		return releaseMethodDataId;
	}

	public void setReleaseMethodDataId(String releaseMethodDataId) {
		this.releaseMethodDataId = releaseMethodDataId;
	}

	public int getAfterOpt() {
		return afterOpt;
	}

	public void setAfterOpt(int afterOpt) {
		this.afterOpt = afterOpt;
	}

	public String getResultDirFile() {
		if (resultDirFile == null) {
			return null;
		}
		return FileUtil.normalize(this.resultDirFile.trim());
	}

	public void setResultDirFile(String resultDirFile) {
		this.resultDirFile = resultDirFile;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	/**
	 * 更新 字段值
	 *
	 * @param buildInfoModel 构建对象
	 */
	public void updateValue(BuildInfoModel buildInfoModel) {
		this.setId(buildInfoModel.getId());
		this.setName(buildInfoModel.getName());
		this.setReleaseMethod(buildInfoModel.getReleaseMethod());
		this.setResultDirFile(buildInfoModel.getResultDirFile());
		this.setWorkspaceId(buildInfoModel.getWorkspaceId());
	}

	public void updateValue(BuildHistoryLog buildHistoryLog) {
		//
		this.setAfterOpt(ObjectUtil.defaultIfNull(buildHistoryLog.getAfterOpt(), AfterOpt.No.getCode()));
		this.setReleaseMethod(buildHistoryLog.getReleaseMethod());
		this.setReleaseCommand(buildHistoryLog.getReleaseCommand());
		this.setReleasePath(buildHistoryLog.getReleasePath());
		this.setReleaseMethodDataId(buildHistoryLog.getReleaseMethodDataId());
		this.setClearOld(ObjectUtil.defaultIfNull(buildHistoryLog.getClearOld(), false));
		this.setResultDirFile(buildHistoryLog.getResultDirFile());
		this.setName(buildHistoryLog.getBuildName());
		//
		this.setId(buildHistoryLog.getBuildDataId());
		this.setWorkspaceId(buildHistoryLog.getWorkspaceId());
		this.setDiffSync(ObjectUtil.defaultIfNull(buildHistoryLog.getDiffSync(), false));
	}

	public void fillLogValue(BuildHistoryLog buildHistoryLog) {
		//
		buildHistoryLog.setAfterOpt(ObjectUtil.defaultIfNull(this.getAfterOpt(), AfterOpt.No.getCode()));
		buildHistoryLog.setReleaseMethod(this.getReleaseMethod());
		buildHistoryLog.setReleaseCommand(this.getReleaseCommand());
		buildHistoryLog.setReleasePath(this.getReleasePath());
		buildHistoryLog.setReleaseMethodDataId(this.getReleaseMethodDataId());
		buildHistoryLog.setClearOld(this.isClearOld());
		buildHistoryLog.setResultDirFile(this.getResultDirFile());
		buildHistoryLog.setDiffSync(this.isDiffSync());

	}
}
