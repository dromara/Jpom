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
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * webhook 投递（流水线专用）
 * <p>
 * 比在线构建的单地址 GET 更进一步：多地址（由配置决定，本类只负责一次投递）、事件过滤
 * （调度器负责）、GET/POST、form/json、超时、失败重试 1 次，并把结果（状态码/异常原因）
 * 交给调用方写进运行日志与 {@code eventsJson}——投递失败可查，不是黑盒。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class PipelineWebhookNotifier {

    /**
     * 连接超时（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 5000;
    /**
     * 读取超时（毫秒）
     */
    private static final int READ_TIMEOUT = 10000;
    /**
     * 最大尝试次数（首次 + 重试 1 次）
     */
    private static final int MAX_ATTEMPTS = 2;

    /**
     * 投递一次 webhook
     *
     * @param hook    钩子配置（地址/方式/请求头）
     * @param payload 参数
     * @return 投递结果
     */
    public Delivery send(PipelineHookDef hook, Map<String, String> payload) {
        Delivery delivery = new Delivery();
        delivery.setUrl(hook.getUrl());
        long startTime = System.currentTimeMillis();
        String lastMessage = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            delivery.setAttempts(attempt);
            try {
                HttpRequest request = this.buildRequest(hook, payload);
                try (HttpResponse response = request.execute()) {
                    int status = response.getStatus();
                    if (status >= 200 && status < 300) {
                        delivery.setSuccess(true);
                        delivery.setMessage(StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_webhook_success.8c15"), status));
                        delivery.setDuration(System.currentTimeMillis() - startTime);
                        return delivery;
                    }
                    lastMessage = StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_webhook_status.8c16"), status,
                        StrUtil.maxLength(StrUtil.nullToEmpty(response.body()), 200));
                }
            } catch (Exception e) {
                lastMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        delivery.setSuccess(false);
        delivery.setMessage(lastMessage);
        delivery.setDuration(System.currentTimeMillis() - startTime);
        return delivery;
    }

    /**
     * 按配置组装请求（GET 走查询串；POST 支持 form / json）
     */
    private HttpRequest buildRequest(PipelineHookDef hook, Map<String, String> payload) {
        String method = StrUtil.emptyToDefault(hook.getMethod(), "POST");
        // hutool 的 form/toParams 只接受 Map<String, Object>，这里统一转一次，避免各处按类型分叉
        Map<String, Object> params = new LinkedHashMap<>();
        params.putAll(payload);
        HttpRequest request;
        if (StrUtil.equalsIgnoreCase(method, "GET")) {
            request = HttpUtil.createGet(hook.getUrl(), true).form(params);
        } else {
            request = HttpUtil.createPost(hook.getUrl());
            if (StrUtil.equalsIgnoreCase(StrUtil.emptyToDefault(hook.getBodyType(), "json"), "form")) {
                request.contentType("application/x-www-form-urlencoded;charset=UTF-8")
                    .body(HttpUtil.toParams(params));
            } else {
                request.contentType("application/json;charset=UTF-8")
                    .body(JSON.toJSONString(payload));
            }
        }
        // 自定义请求头：每行「名称: 值」（例如鉴权头 X-Token: <token>）
        for (String line : StrUtil.split(StrUtil.nullToEmpty(hook.getHeaders()), '\n')) {
            String text = StrUtil.trim(line);
            if (StrUtil.isEmpty(text) || StrUtil.startWith(text, "#")) {
                continue;
            }
            int index = text.indexOf(':');
            if (index <= 0) {
                continue;
            }
            request.header(StrUtil.trim(StrUtil.subPre(text, index)), StrUtil.trim(StrUtil.subSuf(text, index + 1)));
        }
        request.setConnectionTimeout(CONNECT_TIMEOUT).setReadTimeout(READ_TIMEOUT);
        return request;
    }

    /**
     * 投递结果
     */
    @Data
    public static class Delivery {
        private String url;
        private boolean success;
        private int attempts;
        private long duration;
        private String message;
    }
}
