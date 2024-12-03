/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.oauth2.custom;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Lombok;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.utils.Base64Utils;
import me.zhyd.oauth.utils.UrlBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * TOPIAM 认证请求
 *
 * @author SanLi
 * Created by support@topiam.cn on  2024/02/12
 */
public class TopiamAuthOauth2Request extends AuthDefaultRequest {


    public TopiamAuthOauth2Request(AuthConfig config, AuthSource source) {
        super(config, source);
    }


    @Override
    protected AuthToken getAccessToken(AuthCallback authCallback) {
        String body = getAccessToken(authCallback.getCode());
        JSONObject response = JSONObject.parseObject(body);
        checkResponse(response);
        return AuthToken.builder()
                .accessToken(response.getString("access_token"))
                .refreshToken(response.getString("refresh_token"))
                .idToken(response.getString("id_token"))
                .tokenType(response.getString("token_type"))
                .scope(response.getString("scope"))
                .build();
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        String body = doGetUserInfo(authToken);
        JSONObject result = JSONObject.parseObject(body);
        checkResponse(result);
        return AuthUser.builder()
                .uuid(result.getString("sub"))
                .username(result.getString("preferred_username"))
                .nickname(result.getString("nickname"))
                .avatar(result.getString("picture"))
                .email(result.getString("email"))
                .token(authToken)
                .source(source.toString())
                .build();
    }

    @Override
    protected String doGetUserInfo(AuthToken authToken) {
        HttpRequest httpRequest = HttpUtil.createGet(source.userInfo());
        httpRequest.header("Authorization", "Bearer " + authToken.getAccessToken());
        try (HttpResponse execute = httpRequest.execute()) {
            return execute.body();
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    @Override
    public String authorize(String state) {
        return UrlBuilder.fromBaseUrl(super.authorize(state))
                .queryParam("scope", StrUtil.join(" ", this.config.getScopes())).build();
    }

    public static void checkResponse(JSONObject object) {
        // oauth/token 验证异常
        if (object.containsKey("error")) {
            throw new AuthException(object.getString("error_description"));
        }
        // user 验证异常
        if (object.containsKey("message")) {
            throw new AuthException(object.getString("message"));
        }
    }

    protected String getAccessToken(String code) {
        HttpRequest httpRequest = HttpUtil.createPost(source.accessToken());

        httpRequest.header("Authorization", getBasic(config.getClientId(), config.getClientSecret()));
        Map<String, Object> form = new HashMap<>(7);
        form.put("code", code);
        form.put("grant_type", "authorization_code");
        form.put("redirect_uri", config.getRedirectUri());
        httpRequest.form(form);
        try (HttpResponse execute = httpRequest.execute()) {
            return execute.body();
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    private String getBasic(String appKey, String appSecret) {
        StringBuilder sb = new StringBuilder();
        String encodeToString = Base64Utils.encode((appKey + ":" + appSecret).getBytes());
        sb.append("Basic").append(" ").append(encodeToString);
        return sb.toString();
    }

}
