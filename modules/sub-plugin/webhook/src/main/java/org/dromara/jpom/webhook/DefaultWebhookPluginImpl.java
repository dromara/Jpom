/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.webhook;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.plugins.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.plugin.IDefaultPlugin;

import java.util.Map;

/**
 * 默认到 webhook 实现
 *
 * @author bwcx_jzy
 * @since 2021/12/22
 */
@PluginConfig(name = "webhook")
@Slf4j
public class DefaultWebhookPluginImpl implements IDefaultPlugin {

    public enum WebhookEvent {
        /**
         * 构建
         */
        BUILD,
        /**
         * 项目
         */
        PROJECT,
        /**
         * 监控
         */
        MONITOR,
        /**
         * 分发
         */
        DISTRIBUTE,
    }

    @Override
    public Object execute(Object main, Map<String, Object> parameter) {
        String webhook = StrUtil.toStringOrNull(main);
        if (StrUtil.isEmpty(webhook)) {
            return null;
        }
        Object jpomWebhookEvent = parameter.remove("JPOM_WEBHOOK_EVENT");
        if (jpomWebhookEvent instanceof WebhookEvent) {
            WebhookEvent webhookEvent = (WebhookEvent) jpomWebhookEvent;
            log.debug("webhook event: [{}]{}", webhookEvent, webhook);
        }
        try {
            HttpRequest httpRequest = HttpUtil.createGet(webhook, true);
            httpRequest.form(parameter);
            try (HttpResponse execute = httpRequest.execute()) {
                String body = execute.body();
                log.info(webhook + CharPool.COLON + body);
                return body;
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.webhooks_invocation_error.9792"), e);
            return "WebHooks error:" + e.getMessage();
        }
    }
}
