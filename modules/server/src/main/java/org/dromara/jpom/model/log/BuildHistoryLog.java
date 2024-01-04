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
package org.dromara.jpom.model.log;

import cn.hutool.core.annotation.PropIgnore;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.enums.BuildStatus;

import java.util.Map;

/**
 * 构建历史记录
 *
 * @author bwcx_jzy
 * @see BuildExtraModule
 * @since 2019/7/17
 **/
@EqualsAndHashCode(callSuper = true)
@TableName(value = "BUILDHISTORYLOG", name = "构建历史", parents = BuildInfoModel.class)
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
     * 构建产物目录
     */
    private String resultDirFile;

    /**
     * 触发构建类型 触发类型{0，手动，1 触发器,2 自动触发,3 手动回滚}
     */
    private Integer triggerBuildType;

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
     * 来自的构建编号，回滚时存在
     */
    private Integer fromBuildNumberId;
    /**
     * 状态
     *
     * @see BuildStatus
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;
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
     *
     * @see BuildExtraModule
     */
    private String extraData;
    /**
     * 是否存在构建产物
     */
    @PropIgnore
    private Boolean hasFile;
    /**
     * 是否存在日志
     */
    @PropIgnore
    private Boolean hasLog;
    /**
     * 构建环境变量缓存
     */
    private String buildEnvCache;
    /**
     * 产物文件大小
     */
    private Long resultFileSize;

    /**
     * 构建日志文件大小
     */
    private Long buildLogFileSize;

    public void setBuildRemark(String buildRemark) {
        this.buildRemark = StrUtil.maxLength(buildRemark, 240);
    }

    public EnvironmentMapBuilder toEnvironmentMapBuilder() {
        String buildEnvCache = this.getBuildEnvCache();
        JSONObject jsonObject = Opt.ofBlankAble(buildEnvCache).map(JSONObject::parseObject).orElse(new JSONObject());
        Map<String, EnvironmentMapBuilder.Item> map = jsonObject.to(new TypeReference<Map<String, EnvironmentMapBuilder.Item>>() {
        });
        return EnvironmentMapBuilder.builder(map);
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
