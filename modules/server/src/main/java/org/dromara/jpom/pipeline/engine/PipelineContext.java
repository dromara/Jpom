/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.engine;

import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import org.dromara.jpom.pipeline.model.PipelineSource;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 一次流水线运行的上下文
 * <p>
 * 阶段之间只通过这里传状态（阶段产物、变量），不共享静态字段：
 * 同一台 Server 可能同时跑多条流水线，静态状态会串数据。
 *
 * @author jpom
 * @since 2.11.16
 */
@Getter
@Setter
public class PipelineContext {

    /**
     * 流水线 ID
     */
    private String pipelineId;
    /**
     * 流水线名称
     */
    private String pipelineName;
    /**
     * 工作空间
     */
    private String workspaceId;
    /**
     * 运行编号（每条流水线自增）
     */
    private int runNumber;
    /**
     * 本次运行的工作区目录（每个源挂到 mount 子目录）
     */
    private File workspaceDir;
    /**
     * 仓库源
     */
    private List<PipelineSource> sources = new ArrayList<>();
    /**
     * 变量（BUILD 阶段写 BUILD_COMMIT_ID 之类，发布/通知阶段可读）
     */
    private Map<String, String> variables = new LinkedHashMap<>();
    /**
     * 阶段产物：stageId → 产物绝对路径（供后续发布阶段按 from 引用）
     */
    private Map<String, String> stageArtifacts = new LinkedHashMap<>();
    /**
     * 日志出口
     */
    private PipelineLog log = PipelineLog.NOOP;
    /**
     * 操作人（审计/通知用）
     */
    private String userId;
    /**
     * 当前阶段进程回调：非 null 时 caller 用它把进程登记进执行体，取消时才能杀到
     */
    private Consumer<Process> processObserver = process -> {
    };
    /**
     * 当前阶段的取消动作（容器/节点构建没有本地进程，用这个杀）
     */
    private volatile Function<String, Boolean> stageCancel;

    /**
     * 写入变量
     *
     * @param key   变量名
     * @param value 值
     */
    public void putVariable(String key, String value) {
        if (key != null && value != null) {
            this.variables.put(key, value);
        }
    }

    /**
     * 记录阶段产物
     *
     * @param stageId  阶段
     * @param path     产物路径
     */
    public void putArtifact(String stageId, String path) {
        if (stageId != null && path != null) {
            this.stageArtifacts.put(stageId, path);
        }
    }
}