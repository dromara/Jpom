/*
 * spring-boot-justauth - Demo project for Spring Boot JustAuth
 * Copyright © 2022-Present Jinan Yuanchuang Network Technology Co., Ltd. (support@topiam.cn)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dromara.jpom.oauth2.custom;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
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
import org.apache.commons.lang3.StringUtils;

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
        HttpRequest httpRequest = cn.hutool.http.HttpUtil.createGet(source.userInfo());
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
            .queryParam("scope", StringUtils.join(this.config.getScopes(), " ")).build();
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
        HttpRequest httpRequest = cn.hutool.http.HttpUtil.createPost(source.accessToken());

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
