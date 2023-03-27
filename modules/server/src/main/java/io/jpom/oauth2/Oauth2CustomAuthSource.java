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
package io.jpom.oauth2;

import io.jpom.common.ILoadEvent;
import io.jpom.service.system.SystemParametersServer;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author MaxKey
 */
@Configuration
public class Oauth2CustomAuthSource implements AuthSource, ILoadEvent {

    public static final String OAUTH_CONFIG_OAUTH2 = "OAUTH_CONFIG_CUSTOM_OAUTH2";

    private Oauth2CustomConfig oauthConfig;
    private AuthOauth2CustomRequest authOauth2Request;
    private final SystemParametersServer systemParametersServer;


    public Oauth2CustomAuthSource(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    @Override
    public String authorize() {
        Assert.notNull(oauthConfig, "为配置 oauth2");
        return oauthConfig.getAuthorizationUri();
    }

    @Override
    public String accessToken() {
        Assert.notNull(oauthConfig, "为配置 oauth2");
        return oauthConfig.getAccessTokenUri();
    }

    @Override
    public String userInfo() {
        Assert.notNull(oauthConfig, "为配置 oauth2");
        return oauthConfig.getUserInfoUri();
    }


    @Override
    public Class<? extends AuthDefaultRequest> getTargetClass() {
        // TODO Auto-generated method stub
        return AuthOauth2CustomRequest.class;
    }

    public Oauth2CustomConfig getOauth2Config() {
        return oauthConfig;
    }

    public <T> T getValue(Function<Oauth2CustomConfig, T> function, T defaultValue) {
        return Optional.ofNullable(this.oauthConfig).map(function).orElse(defaultValue);
    }

    public AuthOauth2CustomRequest getAuthOauth2Request() {
        Assert.notNull(authOauth2Request, "为配置 oauth2");
        return authOauth2Request;
    }

    public void refreshCache() {
        this.oauthConfig = systemParametersServer.getConfigDefNewInstance(OAUTH_CONFIG_OAUTH2, Oauth2CustomConfig.class);
        if (this.oauthConfig.enabled()) {
            AuthConfig authConfig = AuthConfig.builder()
                .clientId(oauthConfig.getClientId())
                .clientSecret(oauthConfig.getClientSecret())
                .redirectUri(oauthConfig.getRedirectUri())
                .build();
            this.authOauth2Request = new AuthOauth2CustomRequest(authConfig, this);
        } else {
            this.authOauth2Request = null;
        }
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        this.refreshCache();
    }
}
