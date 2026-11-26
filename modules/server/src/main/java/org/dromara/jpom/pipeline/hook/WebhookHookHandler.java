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
import org.dromara.jpom.pipeline.engine.PipelineLog;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 事件钩子：HTTP webhook 回调
 * <p>
 * 投递失败只记日志与记录，不让流水线失败（通知通道不是发布依据，同 NOTIFY 阶段的语义）；
 * 但失败原因一定可见：运行日志一行、{@code eventsJson} 一条。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Component
public class WebhookHookHandler implements PipelineHookHandler {

    private final PipelineWebhookNotifier notifier;

    public WebhookHookHandler(PipelineWebhookNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public String type() {
        return "webhook";
    }

    @Override
    public String handle(PipelineHookDef hook, PipelineHookContext context) {
        PipelineContext pipelineContext = context.getPipelineContext();
        PipelineLog log = pipelineContext == null ? PipelineLog.NOOP : pipelineContext.getLog();
        PipelineEventRecord.WebhookRecord record = new PipelineEventRecord.WebhookRecord();
        record.setUrl(hook.getUrl());
        if (context.getRecord() != null) {
            context.getRecord().getWebhooks().add(record);
        }
        if (StrUtil.isBlank(hook.getUrl())) {
            record.setMessage(PipelineHookMessages.get("i18n.pipeline_hook_url_invalid.8c09"));
            record.setSuccess(false);
            return record.getMessage();
        }
        Map<String, String> payload = PipelineHookPayload.of(context);
        PipelineWebhookNotifier.Delivery delivery = this.notifier.send(hook, payload);
        record.setSuccess(delivery.isSuccess());
        record.setAttempts(delivery.getAttempts());
        record.setDuration(delivery.getDuration());
        record.setMessage(delivery.getMessage());
        if (delivery.isSuccess()) {
            log.info(StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_webhook_done.8c05"), hook.getUrl(), delivery.getAttempts()));
            return null;
        }
        String message = StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_webhook_error.8c06"), hook.getUrl(),
            StrUtil.nullToEmpty(delivery.getMessage()));
        log.error(message);
        return message;
    }
}
