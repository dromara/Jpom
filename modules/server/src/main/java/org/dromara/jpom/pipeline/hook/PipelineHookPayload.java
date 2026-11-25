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

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.pipeline.engine.PipelineContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 事件载荷装配
 * <p>
 * 脚本环境变量与 webhook 参数共用同一份载荷：一套字段名（`PIPELINE_*` 给脚本，驼峰
 * 别名给 webhook 表单），避免脚本与 webhook 各拿一套对不上的字段。运行上下文变量
 * （{@code PIPELINE_SOURCE_*_DIR}、{@code PIPELINE_STAGE_*_ARTIFACT}）自动并入。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineHookPayload {

    private PipelineHookPayload() {
    }

    /**
     * 组装载荷
     *
     * @param context 钩子上下文
     * @return 变量表（脚本用作环境变量；webhook 用作表单/JSON 字段）
     */
    public static Map<String, String> of(PipelineHookContext context) {
        PipelineEvent event = context.getEvent();
        PipelineContext pipelineContext = context.getPipelineContext();
        Map<String, String> payload = new LinkedHashMap<>();
        if (pipelineContext != null) {
            payload.putAll(pipelineContext.getVariables());
        }
        String pipelineId = pipelineContext == null ? StrUtil.EMPTY : StrUtil.nullToEmpty(pipelineContext.getPipelineId());
        String pipelineName = pipelineContext == null ? StrUtil.EMPTY : StrUtil.nullToEmpty(pipelineContext.getPipelineName());
        String runNumber = pipelineContext == null ? StrUtil.EMPTY : String.valueOf(pipelineContext.getRunNumber());
        String runId = pipelineContext == null ? StrUtil.EMPTY : StrUtil.nullToEmpty(pipelineContext.getRunId());
        String status = context.getStatus() == null ? StrUtil.EMPTY : String.valueOf(context.getStatus());
        // 脚本环境变量：大写，与其他阶段注入的 PIPELINE_* 变量同名同义
        payload.put("PIPELINE_ID", pipelineId);
        payload.put("PIPELINE_NAME", pipelineName);
        payload.put("PIPELINE_RUN", runNumber);
        payload.put("PIPELINE_RUN_ID", runId);
        payload.put("PIPELINE_EVENT", event == null ? StrUtil.EMPTY : event.getCode());
        payload.put("PIPELINE_STATUS", status);
        payload.put("PIPELINE_STATUS_MSG", StrUtil.nullToEmpty(context.getStatusMsg()));
        payload.put("PIPELINE_STAGE_ID", StrUtil.nullToEmpty(context.getStageId()));
        payload.put("PIPELINE_STAGE_NAME", StrUtil.nullToEmpty(context.getStageName()));
        payload.put("PIPELINE_STAGE_STATUS", StrUtil.nullToEmpty(context.getStageStatus()));
        payload.put("PIPELINE_TRIGGER_TYPE", context.getTriggerType() == null ? StrUtil.EMPTY : String.valueOf(context.getTriggerType()));
        // webhook 侧的驼峰别名（老在线构建 webhook 的 type 参数语义保留：type == event）
        payload.put("event", event == null ? StrUtil.EMPTY : event.getCode());
        payload.put("type", event == null ? StrUtil.EMPTY : event.getCode());
        payload.put("pipelineId", pipelineId);
        payload.put("pipelineName", pipelineName);
        payload.put("runNumber", runNumber);
        payload.put("runId", runId);
        payload.put("status", status);
        payload.put("statusMsg", StrUtil.nullToEmpty(context.getStatusMsg()));
        payload.put("stageId", StrUtil.nullToEmpty(context.getStageId()));
        payload.put("stageName", StrUtil.nullToEmpty(context.getStageName()));
        payload.put("stageStatus", StrUtil.nullToEmpty(context.getStageStatus()));
        payload.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return payload;
    }
}
