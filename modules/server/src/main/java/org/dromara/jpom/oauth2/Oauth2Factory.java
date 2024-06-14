/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.oauth2;

import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.SafeConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2023/3/30
 */
@Configuration
@Slf4j
public class Oauth2Factory implements ILoadEvent {

    private static final Map<String, AuthRequest> AUTH_REQUEST = new SafeConcurrentHashMap<>();
    private static final Map<String, BaseOauth2Config> AUTH_CONFIG = new SafeConcurrentHashMap<>();

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

    /**
     * 获取配置
     *
     * @param provide 平台
     * @return config
     */
    public static BaseOauth2Config getConfig(String provide) {
        BaseOauth2Config oauth2Config = AUTH_CONFIG.get(provide);
        Assert.notNull(oauth2Config, I18nMessageUtil.get("i18n.no_oauth2_found.ea74") + provide);
        return oauth2Config;
    }

    public static AuthRequest get(String provide) {
        AuthRequest authRequest = AUTH_REQUEST.get(provide);
        Assert.notNull(authRequest, I18nMessageUtil.get("i18n.no_oauth2_found.ea74") + provide);
        return authRequest;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        for (Map.Entry<String, Tuple> entry : BaseOauth2Config.DB_KEYS.entrySet()) {
            Tuple value = entry.getValue();
            String dbKey = value.get(0);
            BaseOauth2Config baseOauth2Config = systemParametersServer.getConfigDefNewInstance(dbKey, value.get(1));
            put(baseOauth2Config);
            log.debug(I18nMessageUtil.get("i18n.load_oauth2_config.da42"), entry.getKey(), dbKey);
        }
    }
}
