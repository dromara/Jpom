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
package io.jpom.model.log;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.util.StrUtil;
import io.jpom.build.BuildExtraModule;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 构建历史记录
 *
 * @author bwcx_jzy
 * @since 2019/7/17
 * @see BuildExtraModule
 **/
@EqualsAndHashCode(callSuper = true)
@TableName(value = "BUILDHISTORYLOG", name = "构建历史")
@Data
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
	@Deprecated
	private String releaseMethodDataId;
	/**
	 * 分发后的操作
	 * 仅在项目发布类型生效
	 *
	 * @see io.jpom.model.AfterOpt
	 * @see BuildInfoModel#getExtraData()
	 */
	@Deprecated
	private Integer afterOpt;
	/**
	 * 是否清空旧包发布
	 */
	@Deprecated
	private Boolean clearOld;
	/**
	 * 构建产物目录
	 */
	private String resultDirFile;
	/**
	 * 发布命令  ssh 才能用上
	 */
	@Deprecated
	private String releaseCommand;
	/**
	 * 发布到ssh中的目录
	 */
	@Deprecated
	private String releasePath;
	/**
	 * 触发构建类型 触发类型{0，手动，1 触发器,2 自动触发}
	 */
	private Integer triggerBuildType;
	/**
	 * 增量同步
	 */
	@Deprecated
	private Boolean diffSync;
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
	 * 构建备注
	 */
	private String buildRemark;
	/**
	 * 构建其他信息
	 */
	private String extraData;
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

	public void setBuildRemark(String buildRemark) {
		this.buildRemark = StrUtil.maxLength(buildRemark, 240);
	}

//	public void fillLogValue(BuildExtraModule buildExtraModule) {
//		//
//		this.setAfterOpt(ObjectUtil.defaultIfNull(buildExtraModule.getAfterOpt(), AfterOpt.No.getCode()));
//		this.setReleaseMethod(buildExtraModule.getReleaseMethod());
//		this.setReleaseCommand(buildExtraModule.getReleaseCommand());
//		this.setReleasePath(buildExtraModule.getReleasePath());
//		this.setReleaseMethodDataId(buildExtraModule.getReleaseMethodDataId());
//		this.setClearOld(buildExtraModule.isClearOld());
//		this.setResultDirFile(buildExtraModule.getResultDirFile());
//		this.setDiffSync(buildExtraModule.isDiffSync());
//	}
}
