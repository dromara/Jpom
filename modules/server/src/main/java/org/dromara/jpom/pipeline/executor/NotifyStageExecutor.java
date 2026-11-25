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

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.StageExecutor;
import org.dromara.jpom.pipeline.engine.StageResult;
import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知阶段执行器（多通知）
 * <p>
 * 配置：{@code scripts}（目标数组，字符串或 {target:...}）、{@code events}。
 * 规则：**单个通知失败不影响其它通知，也不让主流程失败**——通知本身不是构建成败的依据；
 * 失败只记录在阶段消息里（数量 + 失败目标），便于排查。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class NotifyStageExecutor implements StageExecutor {

    private final PipelineNotifier notifier;

    public NotifyStageExecutor(PipelineNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public PipelineStageType type() {
        return PipelineStageType.NOTIFY;
    }

    @Override
    public StageResult execute(PipelineStageDef stage, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> config = stage.getConfig() == null ? new LinkedHashMap<>() : stage.getConfig();
        List<String> targets = parseTargets(config.get("scripts"));
        if (targets.isEmpty()) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SUCCESS, "no notify target", startTime);
        }
        String event = StrUtil.emptyToDefault((String) config.get("event"), "run_finished");
        Map<String, String> payload = new LinkedHashMap<>(context.getVariables());
        payload.put("PIPELINE_ID", StrUtil.nullToEmpty(context.getPipelineId()));
        payload.put("PIPELINE_NAME", StrUtil.nullToEmpty(context.getPipelineName()));
        payload.put("PIPELINE_RUN", String.valueOf(context.getRunNumber()));
        payload.put("PIPELINE_STAGE", StrUtil.nullToEmpty(stage.getId()));
        List<String> failed = new ArrayList<>();
        for (String target : targets) {
            try {
                String error = this.notifier.notify(target, event, payload);
                if (error != null) {
                    failed.add(target + ": " + error);
                }
            } catch (Exception e) {
                failed.add(target + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            }
        }
        String message = "通知 " + (targets.size() - failed.size()) + "/" + targets.size() + " 成功"
            + (failed.isEmpty() ? "" : "，失败：" + String.join("; ", failed));
        // 通知失败不让流水线失败：这是运维告警通道，不是发布依据
        return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SUCCESS, message, startTime);
    }

    private static List<String> parseTargets(Object value) {
        List<String> targets = new ArrayList<>();
        if (value == null) {
            return targets;
        }
        List<String> list = JSON.parseArray(JSON.toJSONString(value), String.class);
        if (list != null) {
            targets.addAll(list);
        }
        return targets;
    }
}
