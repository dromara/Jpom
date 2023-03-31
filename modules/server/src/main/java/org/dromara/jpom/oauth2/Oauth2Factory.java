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
package org.dromara.jpom.oauth2;

import cn.hutool.core.lang.Tuple;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.service.system.SystemParametersServer;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bwcx_jzy
 * @since 2023/3/30
 */
@Configuration
@Slf4j
public class Oauth2Factory implements ILoadEvent {

    private static final Map<String, AuthRequest> AUTH_REQUEST = new ConcurrentHashMap<>();
    private static final Map<String, BaseOauth2Config> AUTH_CONFIG = new ConcurrentHashMap<>();

    private final SystemParametersServer systemParametersServer;

    public Oauth2Factory(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 查询已经开启的平台
     *
     * @return 平台名称
     */
    public static Collection<String> provides() {
        return AUTH_CONFIG.keySet();
    }

    /**
     * 更新配置
     *
     * @param oauth2Config 配置
     */
    public static void put(BaseOauth2Config oauth2Config) {
        if (oauth2Config.enabled()) {
            AUTH_REQUEST.put(oauth2Config.provide(), oauth2Config.authRequest());
            AUTH_CONFIG.put(oauth2Config.provide(), oauth2Config);
        } else {
            AUTH_REQUEST.remove(oauth2Config.provide());
            AUTH_CONFIG.remove(oauth2Config.provide());
        }
    }

    public static BaseOauth2Config getConfig(String provide) {
        BaseOauth2Config oauth2Config = AUTH_CONFIG.get(provide);
        Assert.notNull(oauth2Config, "没有找到对应的 oauth2," + provide);
        return oauth2Config;
    }

    public static AuthRequest get(String provide) {
        AuthRequest authRequest = AUTH_REQUEST.get(provide);
        Assert.notNull(authRequest, "没有找到对应的 oauth2," + provide);
        return authRequest;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        for (Map.Entry<String, Tuple> entry : BaseOauth2Config.DB_KEYS.entrySet()) {
            Tuple value = entry.getValue();
            String dbKey = value.get(0);
            BaseOauth2Config baseOauth2Config = systemParametersServer.getConfigDefNewInstance(dbKey, value.get(1));
            put(baseOauth2Config);
            log.debug("加载 oauth2 配置 ：{} {}", entry.getKey(), dbKey);
        }
    }
}
