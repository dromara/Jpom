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

import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;

/**
 * 阶段执行器契约
 * <p>
 * 一种阶段类型 = 一个实现类 + 在 {@link StageExecutorRegistry} 注册一行；
 * 引擎只认 {@link #type()} 与 {@link #execute}，不再用 if/else 分派。
 *
 * @author jpom
 * @since 2.11.16
 */
public interface StageExecutor {

    /**
     * 支持的阶段类型
     *
     * @return 类型
     */
    PipelineStageType type();

    /**
     * 执行阶段
     *
     * @param stage   阶段定义
     * @param context 运行上下文
     * @return 执行结果（不返回 null）
     * @throws Exception 执行异常（引擎按失败处理，并记录原因）
     */
    StageResult execute(PipelineStageDef stage, PipelineContext context) throws Exception;
}
