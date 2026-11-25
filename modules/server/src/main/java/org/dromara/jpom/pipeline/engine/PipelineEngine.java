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

import org.dromara.jpom.pipeline.model.PipelineDefinition;
import org.dromara.jpom.pipeline.model.PipelineStageDef;

import java.util.ArrayList;
import java.util.List;

/**
 * 流水线引擎（顺序执行 + 失败策略 + 取消）
 * <p>
 * 刻意做成纯逻辑：不依赖 Spring、不碰数据库、不碰进程——执行器（StageExecutor）才是干活的那层。
 * 这样「顺序、失败即停/继续、取消、禁用阶段、类型未注册」这些规则都能在单测里确定性验证，
 * 不必起容器或真实目标机。
 * <p>
 * 取消语义：每个阶段边界检查一次取消信号（不打断正在跑的阶段——那是执行器自己的事），
 * 一旦取消，剩余阶段标 CANCELLED，绝不标成功。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineEngine {

    /**
     * 取消信号
     */
    @FunctionalInterface
    public interface CancelSignal {
        /**
         * @return true 表示已请求取消
         */
        boolean isCancelled();

        /**
         * 永不取消
         */
        CancelSignal NONE = () -> false;
    }

    /**
     * 执行整条流水线
     *
     * @param definition 定义
     * @param context    上下文
     * @param signal     取消信号
     * @return 运行结果（每个阶段都有明确状态）
     */
    public PipelineRunResult run(PipelineDefinition definition, PipelineContext context, CancelSignal signal) {
        PipelineLog log = context.getLog();
        List<StageResult> results = new ArrayList<>();
        List<PipelineStageDef> stages = definition.getStages();
        boolean blocked = false;
        for (PipelineStageDef stage : stages) {
            long startTime = System.currentTimeMillis();
            if (!stage.enabled()) {
                StageResult skipped = StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SKIPPED, "disabled", startTime);
                results.add(skipped);
                log.info("阶段已禁用，跳过：" + stage.getName());
                continue;
            }
            if (signal.isCancelled()) {
                StageResult cancelled = StageResult.of(stage.getId(), stage.getName(), StageResult.Status.CANCELLED, "cancelled", startTime);
                results.add(cancelled);
                log.info("已取消，跳过阶段：" + stage.getName());
                continue;
            }
            if (blocked) {
                StageResult skipped = StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SKIPPED, "blocked by previous failure", startTime);
                results.add(skipped);
                log.info("前置阶段失败（失败即停），跳过阶段：" + stage.getName());
                continue;
            }
            results.add(this.runStage(stage, context, log, startTime));
            StageResult last = results.get(results.size() - 1);
            if (last.getStatus() == StageResult.Status.FAILED && stage.failFast()) {
                blocked = true;
            }
        }
        return PipelineRunResult.of(results);
    }

    /**
     * 执行单个阶段（异常一律转失败，不向上抛，保证后续阶段状态明确）
     */
    private StageResult runStage(PipelineStageDef stage, PipelineContext context, PipelineLog log, long startTime) {
        StageExecutor executor = StageExecutorRegistry.get(stage.getType());
        if (executor == null) {
            String message = "阶段类型未注册执行器：" + stage.getType();
            log.error(message);
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, message, startTime);
        }
        log.info("开始执行阶段：" + stage.getName() + "（" + stage.getType() + "）");
        try {
            StageResult result = executor.execute(stage, context);
            if (result == null) {
                String message = "执行器未返回结果：" + stage.getType();
                log.error(message);
                return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, message, startTime);
            }
            if (result.getStatus() == StageResult.Status.FAILED) {
                log.error("阶段失败：" + stage.getName() + " " + result.getMessage());
            } else {
                log.info("阶段结束：" + stage.getName() + " " + result.getStatus());
            }
            return result;
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("阶段异常：" + stage.getName() + " " + message);
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, message, startTime);
        }
    }
}
