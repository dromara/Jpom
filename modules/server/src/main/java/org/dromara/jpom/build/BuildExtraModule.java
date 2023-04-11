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
package org.dromara.jpom.build;

import cn.hutool.core.io.FileUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseModel;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.util.StringUtil;
import org.springframework.util.Assert;

import java.io.File;

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
     * @see AfterOpt
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
     * 发布后命令  ssh 才能用上
     */
    private String releaseCommand;
    /**
     * 发布前命令  ssh 才能用上
     */
    private String releaseBeforeCommand;
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

    /**
     * 是否执行推送到仓库中
     */
    private Boolean pushToRepository;
    /**
     * docker tag 版本字段递增
     */
    private Boolean dockerTagIncrement;

    /**
     * 附加环境变量,比如常见的 .env 文件
     */
    private String attachEnv;

    /**
     * 容器构建参数 如：key1=values1&keyvalue2
     */
    private String dockerBuildArgs;

    /**
     * 构建镜像尝试去更新基础镜像的新版本
     */
    private Boolean dockerBuildPull;
    /**
     * 构建镜像的过程不使用缓存
     */
    private Boolean dockerNoCache;
    /**
     * 镜像标签
     */
    private String dockerImagesLabels;
    /**
     * 项目二级目录
     */
    private String projectSecondaryDirectory;

    /**
     * 保存项目文件前先关闭
     */
    private Boolean projectUploadCloseFirst;

    /**
     * 是否为严格执行脚本，严格执行脚本执行结果返回状态码必须是 0
     */
    private Boolean strictlyEnforce;
    /**
     * 是否同步到文件管理中心
     */
    private Boolean syncFileStorage;

    /**
     * 排除指定目录发布
     */
    private String excludeReleaseAnt;

    /**
     * 是否发布隐藏文件
     */
    private Boolean releaseHideFile;

    /**
     * 克隆深度
     */
    private Integer cloneDepth;

    public boolean strictlyEnforce() {
        return strictlyEnforce != null && strictlyEnforce;
    }

    public String getResultDirFile() {
        if (resultDirFile == null) {
            return null;
        }
        return FileUtil.normalize(this.resultDirFile.trim());
    }

    public File resultDirFile(int buildNumberId) {
        return BuildUtil.getHistoryPackageFile(this.getId(), buildNumberId, this.getResultDirFile());
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
        Assert.notNull(buildExtraModule, "数据不完整，暂不支持操作");
        buildExtraModule.setId(buildHistoryLog.getBuildDataId());
        buildExtraModule.setName(buildHistoryLog.getBuildName());
        buildExtraModule.setReleaseMethod(buildHistoryLog.getReleaseMethod());
        buildExtraModule.setResultDirFile(buildHistoryLog.getResultDirFile());
        buildExtraModule.setWorkspaceId(buildHistoryLog.getWorkspaceId());
        //
        return buildExtraModule;
    }
}
