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

import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.oauth2.platform.MaxKeyOauth2Config;
import org.springframework.util.Assert;

/**
 * @author MaxKey
 */
public class Oauth2MaxKeyAuthSource implements AuthSource {

    private final MaxKeyOauth2Config oauthConfig;

    public Oauth2MaxKeyAuthSource(MaxKeyOauth2Config oauthConfig) {
        this.oauthConfig = oauthConfig;
    }

    @Override
    public String authorize() {
        Assert.notNull(oauthConfig, I18nMessageUtil.get("i18n.oauth2_not_configured.9c85"));
        return oauthConfig.getAuthorizationUri();
    }

    @Override
    public String accessToken() {
        Assert.notNull(oauthConfig, I18nMessageUtil.get("i18n.oauth2_not_configured.9c85"));
        return oauthConfig.getAccessTokenUri();
    }

    @Override
    public String userInfo() {
        Assert.notNull(oauthConfig, I18nMessageUtil.get("i18n.oauth2_not_configured.9c85"));
        return oauthConfig.getUserInfoUri();
    }


    @Override
    public Class<? extends AuthDefaultRequest> getTargetClass() {
        // TODO Auto-generated method stub
        return AuthOauth2MaxKeyRequest.class;
    }
}
