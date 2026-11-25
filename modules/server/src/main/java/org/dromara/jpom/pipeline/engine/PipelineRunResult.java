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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次流水线运行的结果汇总
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineRunResult {

    private final List<StageResult> stages;

    private PipelineRunResult(List<StageResult> stages) {
        this.stages = Collections.unmodifiableList(stages);
    }

    static PipelineRunResult of(List<StageResult> stages) {
        return new PipelineRunResult(stages);
    }

    /**
     * @return 各阶段结果（按执行顺序）
     */
    public List<StageResult> getStages() {
        return stages;
    }

    /**
     * @return 是否全部成功（跳过/取消都算未成功）
     */
    public boolean isSuccess() {
        for (StageResult stage : stages) {
            if (stage.getStatus() != StageResult.Status.SUCCESS) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return 失败阶段数
     */
    public long failedCount() {
        return stages.stream().filter(stage -> stage.getStatus() == StageResult.Status.FAILED).count();
    }

    /**
     * @return 被取消的阶段数
     */
    public long cancelledCount() {
        return stages.stream().filter(stage -> stage.getStatus() == StageResult.Status.CANCELLED).count();
    }

    /**
     * @return 第一个失败阶段（没有返回 null）
     */
    public StageResult firstFailure() {
        for (StageResult stage : stages) {
            if (stage.getStatus() == StageResult.Status.FAILED) {
                return stage;
            }
        }
        return null;
    }

    /**
     * 便于测试与日志的摘要
     *
     * @return 摘要
     */
    public String summary() {
        List<String> parts = new ArrayList<>();
        for (StageResult stage : stages) {
            parts.add(stage.getStageName() + ":" + stage.getStatus());
        }
        return String.join(", ", parts);
    }
}
