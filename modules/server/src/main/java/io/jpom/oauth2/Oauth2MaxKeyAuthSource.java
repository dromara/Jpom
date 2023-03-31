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

import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;
import org.springframework.util.Assert;

/**
 * @author MaxKey
 */
public class Oauth2MaxKeyAuthSource implements AuthSource {

    private final Oauth2MaxKeyConfig oauthConfig;

    public Oauth2MaxKeyAuthSource(Oauth2MaxKeyConfig oauthConfig) {
        this.oauthConfig = oauthConfig;
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
        return AuthOauth2MaxKeyRequest.class;
    }
}
