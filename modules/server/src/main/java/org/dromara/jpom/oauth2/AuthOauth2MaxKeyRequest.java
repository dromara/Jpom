/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.oauth2;

import com.alibaba.fastjson2.JSONObject;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;

/**
 * @author MaxKey
 */
public class AuthOauth2MaxKeyRequest extends AuthDefaultRequest {

    public AuthOauth2MaxKeyRequest(AuthConfig config, AuthSource source) {
        super(config, source);
    }

    @Override
    protected AuthToken getAccessToken(AuthCallback authCallback) {
        String body = doPostAuthorizationCode(authCallback.getCode());
        JSONObject object = JSONObject.parseObject(body);
        return AuthToken.builder()
            .accessToken(object.getString("access_token"))
            .refreshToken(object.getString("refresh_token"))
            .idToken(object.getString("id_token"))
            .tokenType(object.getString("token_type"))
            .scope(object.getString("scope"))
            .build();
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        String body = doGetUserInfo(authToken);
        JSONObject object = JSONObject.parseObject(body);
        return AuthUser.builder()
            .uuid(object.getString("id"))
            .username(object.getString("username"))
            .nickname(object.getString("name"))
            .company(object.getString("organization"))
            .email(object.getString("email"))
            .token(authToken)
            .source("jpom")
            .build();
    }

}
