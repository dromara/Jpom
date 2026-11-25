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

import org.dromara.jpom.pipeline.engine.PipelineWorkspace;
import org.dromara.jpom.pipeline.model.PipelineStageType;

/**
 * 命令阶段执行器：与构建阶段同一条执行链，只是不声明产物
 * <p>
 * 复用 {@link BuildStageExecutor} 的执行逻辑（工作目录、命令、超时/失败判定、取消登记），
 * 避免为「命令」再写一套；区别只在阶段类型与产物语义。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class CommandStageExecutor extends BuildStageExecutor {

    public CommandStageExecutor(PipelineCommandRunner commandRunner, PipelineWorkspace workspace,
                                PipelineContainerBuildRunner containerRunner, PipelineNodeBuildRunner nodeBuildRunner) {
        super(commandRunner, workspace, containerRunner, nodeBuildRunner);
    }

    @Override
    public PipelineStageType type() {
        return PipelineStageType.COMMAND;
    }
}
