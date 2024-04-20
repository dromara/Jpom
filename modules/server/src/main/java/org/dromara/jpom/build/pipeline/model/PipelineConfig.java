/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build.pipeline.model;

import lombok.Data;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.data.BuildInfoModel;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/7
 */
@Data
public class PipelineConfig {

    private Map<String, Repository> repositories;

    private List<IStage> stages;

    public interface IStage {
        StageType getStageType();
    }

    public enum StageType {
        EXEC,
        PUBLISH
    }

    @Data
    public static class Publish implements IStage {
        /**
         * 阶段类型
         */
        private StageType stageType;
        /**
         * 执行描述
         */
        private String desc;
        /**
         * 执行的脚本
         */
        private String commands;
    }

    @Data
    public static class BaseStage implements IStage {
        /**
         * 阶段类型
         */
        private StageType stageType;
        /**
         * 执行的目录
         * <p>
         * 仓库的标记
         *
         * @see PipelineConfig#getRepositories()
         */
        private String repoTag;
    }

    @Data
    public static class PublishByProject {

        private String nodeId;

        private String projectId;

        private String projectSecondaryDirectory;

        /**
         * 保存项目文件前先关闭
         */
        private Boolean projectUploadCloseFirst;

        /**
         * 分发后的操作
         * 仅在项目发布类型生效
         *
         * @see AfterOpt
         * @see BuildInfoModel#getExtraData()
         */
        private int afterOpt;
    }

    @Data
    public static class ExecCommand implements IStage {
        /**
         * 阶段类型
         */
        private StageType stageType;
        /**
         * 执行描述
         */
        private String desc;
        /**
         * 执行的脚本
         */
        private String commands;
        /**
         * 环境变量
         */
        private Map<String, String> env;

        /**
         * 脚本执行超时时间
         */
        private Integer timeout;

        /**
         * 产物
         */
        private List<ArtifactItem> artifacts;
    }

    @Data
    private static class ArtifactItem {
        private String id;

        private List<String> path;
    }

    @Data
    public static class Repository {
        /**
         * 仓库ID
         */
        private String repositoryId;
        /**
         * 分支
         */
        private String branchName;
        /**
         * 标签
         */
        private String branchTagName;
        /**
         * 克隆深度
         */
        private Integer cloneDepth;
        /**
         * 工作目录
         */
        private String workPath;
    }
}
