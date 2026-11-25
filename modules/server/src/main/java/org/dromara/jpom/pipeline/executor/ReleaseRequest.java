/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.executor;

import lombok.Data;
import org.dromara.jpom.util.LogRecorder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单个发布目标的请求
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class ReleaseRequest {
    /**
     * 发布方式码（见 {@code BuildReleaseMethod}）
     */
    private int method;
    /**
     * 目标 ID（项目 / SSH / SSH 项目 / FTP / 分发 / 容器集群服务）
     */
    private String targetId;
    /**
     * 产物目录（由构建阶段产出）
     */
    private String artifactPath;
    /**
     * 产物来源阶段
     */
    private String fromStageId;
    /**
     * 变量（发布前后命令可用）
     */
    private Map<String, String> variables = new LinkedHashMap<>();

    /**
     * 流水线 ID（复用为「构建 ID」，让发布实现按既有布局找产物）
     */
    private String pipelineId;
    /**
     * 流水线名称（文件中心产物的名称/描述要能区分是哪条流水线的第几次运行）
     */
    private String pipelineName;
    /**
     * 运行编号（复用为「构建编号」）
     */
    private int runNumber;
    /**
     * 工作空间
     */
    private String workspaceId;
    /**
     * 目标附加配置（发布目录、发布前后命令、afterOpt 等，见 BuildExtraModule 字段）
     */
    private Map<String, Object> extra = new LinkedHashMap<>();
    /**
     * 运行日志（不参与序列化）
     */
    private transient LogRecorder logRecorder;
}