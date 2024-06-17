/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Tolerate;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseGroupModel;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.util.StringUtil;

/**
 * new BuildModel class, for replace old BuildModel
 *
 * @author Hotstrip
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "BUILD_INFO",
    nameKey = "i18n.build_info.224a")
@Data
@Builder
public class BuildInfoModel extends BaseGroupModel {

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
     *
     * @see BuildStatus
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;
    /**
     * 触发器token
     */
    private String triggerToken;
    /**
     * 额外信息，JSON 字符串格式
     *
     * @see BuildExtraModule
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
    /**
     * 源码目录是否存在
     */
    @PropIgnore
    private Boolean sourceDirExist;
    /**
     * 产物文件是否存在
     */
    @PropIgnore
    private Boolean resultHasFile;
    /**
     * 构建方式 0 本地构建 1 docker 构建
     */
    private Integer buildMode;
    /**
     * 仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）
     */
    private String repositoryLastCommitId;
    /**
     * 排序
     */
    private Float sortValue;
    /**
     * 构建环境变量
     */
    private String buildEnvParameter;
    /**
     * 别名码
     */
    private String aliasCode;
    /**
     * 产物保留天数
     */
    private Integer resultKeepDay;

    @Tolerate
    public BuildInfoModel() {
    }

    /**
     * 获取构建的扩展数据
     *
     * @return extraData
     */
    public BuildExtraModule extraData() {
        BuildExtraModule buildExtraModule = StringUtil.jsonConvert(this.getExtraData(), BuildExtraModule.class);
        if (buildExtraModule != null) {
            if (this.releaseMethodDataId != null) {
                // 兼容数据迁移
                buildExtraModule.setReleaseMethodDataId(this.releaseMethodDataId);
            }
        }
        return buildExtraModule;
    }

    public static String getBuildIdStr(int buildId) {
        return String.format("#%s", buildId);
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
