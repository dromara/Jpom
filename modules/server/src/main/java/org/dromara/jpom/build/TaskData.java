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

import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.user.UserModel;
import lombok.Builder;

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
