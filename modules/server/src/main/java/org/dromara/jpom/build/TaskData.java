/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import lombok.Builder;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.user.UserModel;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Builder
public class TaskData {

    protected final BuildInfoModel buildInfoModel;
    protected final RepositoryModel repositoryModel;
    protected final UserModel userModel;
    /**
     * 延迟执行的时间（单位秒）
     */
    protected final Integer delay;
    /**
     * 触发类型
     * 0: "手动",
     * 1: "触发器",
     * 2: "定时",
     */
    protected final int triggerBuildType;
    /**
     * 构建备注
     */
    protected String buildRemark;
    /**
     * 环境变量
     * 工作空间环境变量
     */
    protected EnvironmentMapBuilder environmentMapBuilder;

    /**
     * 仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）
     */
    protected String repositoryLastCommitId;
    /**
     * 是否差异构建
     */
    protected Boolean checkRepositoryDiff;
    /**
     * 产物文件大小
     */
    protected Long resultFileSize;

    protected Map<String, Object> dockerParameter;

    protected String buildContainerId;
}
