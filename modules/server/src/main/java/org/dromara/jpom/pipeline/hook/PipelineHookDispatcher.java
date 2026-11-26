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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.PipelineLog;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 事件调度器：事件 → 匹配钩子 → 交给对应 {@link PipelineHookHandler} 投递
 * <p>
 * 「事件状态切换机制」的执行端：只做匹配与结果汇总，不做具体投递；单条钩子异常不影响其它钩子，
 * 也不让流水线失败（钩子是旁路能力），但结果一定落到 {@link PipelineEventRecord} 与运行日志。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Slf4j
@Service
public class PipelineHookDispatcher {

    private final PipelineHookHandlerRegistry registry;

    public PipelineHookDispatcher(List<PipelineHookHandler> handlers) {
        this.registry = new PipelineHookHandlerRegistry(handlers);
    }

    /**
     * 触发一次事件
     *
     * @param hooks   流水线配置的钩子（可为空）
     * @param context 事件上下文（event / 阶段 / 状态 / 进程登记）
     * @return 事件记录；没有任何匹配钩子时返回 null（不写 {@code eventsJson}）
     */
    public PipelineEventRecord fire(List<PipelineHookDef> hooks, PipelineHookContext context) {
        PipelineEvent event = context.getEvent();
        if (CollUtil.isEmpty(hooks) || event == null) {
            return null;
        }
        PipelineContext pipelineContext = context.getPipelineContext();
        PipelineLog pipelineLog = pipelineContext == null ? PipelineLog.NOOP : pipelineContext.getLog();
        PipelineEventRecord record = new PipelineEventRecord();
        record.setEvent(event.getCode());
        record.setStageId(context.getStageId());
        record.setStageName(context.getStageName());
        record.setStatus(context.getStatus());
        context.setRecord(record);
        int matched = 0;
        for (PipelineHookDef hook : hooks) {
            if (!hook.match(event)) {
                continue;
            }
            matched++;
            PipelineHookHandler handler = this.registry.get(hook.getType());
            String target = StrUtil.isBlank(hook.getScriptId()) ? StrUtil.nullToEmpty(hook.getUrl()) : hook.getScriptId();
            if (handler == null) {
                String message = StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_type_unknown.8c01"),
                    StrUtil.nullToEmpty(hook.getType()));
                pipelineLog.error(message);
                record.getErrors().add(this.error(hook.getType(), target, message));
                continue;
            }
            int before = record.getScripts().size() + record.getWebhooks().size() + record.getErrors().size();
            try {
                String error = handler.handle(hook, context);
                if (error != null) {
                    log.warn("pipeline hook failed, event:{} type:{} target:{} message:{}", event.getCode(),
                        hook.getType(), target, error);
                    // 实现层已经写了具体记录（脚本/webhook 结果里带失败原因）时不再重复记 error，
                    // 否则一条失败钩子会被统计成两条（dev 实测：1 条 webhook 失败显示「钩子 2 个」）
                    int after = record.getScripts().size() + record.getWebhooks().size() + record.getErrors().size();
                    if (after == before) {
                        record.getErrors().add(this.error(hook.getType(), target, error));
                    }
                }
            } catch (Exception e) {
                // 单条钩子异常不影响其它钩子（事件是旁路能力）
                log.warn("pipeline hook error, event:{} type:{}", event.getCode(), hook.getType(), e);
                String message = StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_exception.8c17"),
                    event.getCode(), StrUtil.nullToEmpty(e.getMessage()));
                pipelineLog.error(message);
                record.getErrors().add(this.error(hook.getType(), target, message));
            }
        }
        if (matched == 0) {
            return null;
        }
        pipelineLog.info(StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_summary.8e02"),
            event.getCode(), record.hookCount(), record.allSuccess() ? "ok" : "partial"));
        return record;
    }

    /**
     * 组装失败记录
     */
    private PipelineEventRecord.HookError error(String type, String target, String message) {
        PipelineEventRecord.HookError error = new PipelineEventRecord.HookError();
        error.setType(type);
        error.setTarget(target);
        error.setMessage(message);
        return error;
    }

    /**
     * @return 已注册的投递方式（诊断用）
     */
    public PipelineHookHandlerRegistry getRegistry() {
        return registry;
    }
}
