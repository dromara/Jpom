/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.hook;

import lombok.Data;
import org.dromara.jpom.pipeline.engine.PipelineContext;

import java.util.function.Consumer;

/**
 * 事件钩子执行上下文
 * <p>
 * 把「事件 + 运行上下文 + 阶段信息 + 记录对象」一次性传给钩子实现，钩子实现不再各自
 * 去拼运行状态；取消相关的进程登记走 {@link #processSink}，事件脚本因此也能被「停止」杀掉。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class PipelineHookContext {

    /**
     * 当前事件
     */
    private PipelineEvent event;
    /**
     * 运行上下文（变量、日志、工作区）
     */
    private PipelineContext pipelineContext;
    /**
     * 阶段 ID（运行级事件为空）
     */
    private String stageId;
    /**
     * 阶段名称（运行级事件为空）
     */
    private String stageName;
    /**
     * 阶段状态（运行级事件为空）
     */
    private String stageStatus;
    /**
     * 当前运行状态码
     */
    private Integer status;
    /**
     * 当前运行状态说明
     */
    private String statusMsg;
    /**
     * 触发方式（0 手动 / 2 系统 / 3 重跑）
     */
    private Integer triggerType;
    /**
     * 进程登记回调（取消时杀进程用）
     */
    private Consumer<Process> processSink = process -> {
    };
    /**
     * 本次事件记录（钩子实现往里追加自己的结果）
     */
    private PipelineEventRecord record;
}
