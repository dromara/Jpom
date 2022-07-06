/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.webhook;

import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;
import lombok.extern.slf4j.Slf4j;

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

    @Override
    public Object execute(Object main, Map<String, Object> parameter) {
        String webhook = StrUtil.toString(main);
        if (StrUtil.isEmpty(webhook)) {
            return null;
        }
        try {
            HttpRequest httpRequest = HttpUtil.createGet(webhook);
            httpRequest.form(parameter);
            try (HttpResponse execute = httpRequest.execute()) {
                String body = execute.body();
                log.info(webhook + CharPool.COLON + body);
                return body;
            }
        } catch (Exception e) {
            log.error("WebHooks 调用错误", e);
            return "WebHooks error:" + e.getMessage();
        }
    }
}
