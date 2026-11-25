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

import lombok.Getter;

/**
 * 阶段执行结果
 * <p>
 * 与取消三态同源：只记录「实际发生了什么」，不猜成功。发布失败、进程被杀、被跳过都要能区分，
 * 这样运行详情才能如实告诉用户卡在哪个阶段、哪个目标。
 *
 * @author jpom
 * @since 2.11.16
 */
@Getter
public final class StageResult {

    /**
     * 阶段状态
     */
    public enum Status {
        /**
         * 成功
         */
        SUCCESS,
        /**
         * 失败（命令非 0、发布目标失败等）
         */
        FAILED,
        /**
         * 未执行（前置阶段失败 + 失败即停，或阶段被禁用）
         */
        SKIPPED,
        /**
         * 被取消（用户取消后不再进入该阶段）
         */
        CANCELLED,
    }

    private final String stageId;
    private final String stageName;
    private final Status status;
    private final String message;
    private final long startTime;
    private final long endTime;

    private StageResult(String stageId, String stageName, Status status, String message, long startTime, long endTime) {
        this.stageId = stageId;
        this.stageName = stageName;
        this.status = status;
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static StageResult of(String stageId, String stageName, Status status, String message, long startTime) {
        return new StageResult(stageId, stageName, status, message, startTime, System.currentTimeMillis());
    }

    /**
     * @return 耗时（毫秒）
     */
    public long duration() {
        return Math.max(0, endTime - startTime);
    }

    @Override
    public String toString() {
        return stageName + "[" + status + "]" + (message == null ? "" : " " + message);
    }
}
