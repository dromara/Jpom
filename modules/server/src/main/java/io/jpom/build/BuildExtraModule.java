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
import io.jpom.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 构建物基类
 *
 * @author bwcx_jzy
 * @since 2019/7/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
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

    private String fromTag;

    private String dockerfile;

    private String dockerTag;
    /**
     * docker 集群ID
     */
    private String dockerSwarmId;
    /**
     * 集群服务名
     */
    private String dockerSwarmServiceName;
    /**
     * 缓存构建目录
     */
    private Boolean cacheBuild;
    /**
     * 是否保留构建历史产物
     */
    private Boolean saveBuildFile;

    /**
     * 构建的时候判断仓库代码是否有变动，true 表示需要判断代码有变动才触发构建
     */
    private Boolean checkRepositoryDiff;

    /**
     * 事件通知执行的脚本 ID
     */
    private String noticeScriptId;

    public String getResultDirFile() {
        if (resultDirFile == null) {
            return null;
        }
        return FileUtil.normalize(this.resultDirFile.trim());
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

    public static BuildExtraModule build(BuildHistoryLog buildHistoryLog) {
        BuildExtraModule buildExtraModule = StringUtil.jsonConvert(buildHistoryLog.getExtraData(), BuildExtraModule.class);
        if (buildExtraModule == null) {
            buildExtraModule = new BuildExtraModule();
            buildExtraModule.setAfterOpt(ObjectUtil.defaultIfNull(buildHistoryLog.getAfterOpt(), AfterOpt.No.getCode()));

            buildExtraModule.setReleaseCommand(buildHistoryLog.getReleaseCommand());
            buildExtraModule.setReleasePath(buildHistoryLog.getReleasePath());
            buildExtraModule.setReleaseMethodDataId(buildHistoryLog.getReleaseMethodDataId());
            buildExtraModule.setClearOld(ObjectUtil.defaultIfNull(buildHistoryLog.getClearOld(), false));
            //
            buildExtraModule.setDiffSync(ObjectUtil.defaultIfNull(buildHistoryLog.getDiffSync(), false));
        }
        buildExtraModule.setId(buildHistoryLog.getBuildDataId());
        buildExtraModule.setName(buildHistoryLog.getBuildName());
        buildExtraModule.setReleaseMethod(buildHistoryLog.getReleaseMethod());
        buildExtraModule.setResultDirFile(buildHistoryLog.getResultDirFile());
        buildExtraModule.setWorkspaceId(buildHistoryLog.getWorkspaceId());
        //
        return buildExtraModule;
    }
}
