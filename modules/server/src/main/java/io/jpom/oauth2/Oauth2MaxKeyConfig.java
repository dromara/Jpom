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

import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhyd.oauth.request.AuthRequest;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2023/3/26
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class Oauth2MaxKeyConfig extends BaseOauth2Config {
    public static final String KEY = "OAUTH_CONFIG_CUSTOM_OAUTH2";

    private String authorizationUri;
    private String accessTokenUri;
    private String userInfoUri;


    /**
     * 验证数据
     */
    public void check() {
        super.check();
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.authorizationUri, "请配置正确的授权 url");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.accessTokenUri, "请配置正确的令牌 url");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.userInfoUri, "请配置正确的用户信息 url");
    }

    @Override
    public String provide() {
        return "maxkey";
    }

    public AuthRequest authRequest() {
        Assert.state(this.enabled(), "没有开启此 " + this.provide() + " oauth2");
        Oauth2MaxKeyAuthSource oauth2MaxKeyAuthSource = new Oauth2MaxKeyAuthSource(this);
        return new AuthOauth2MaxKeyRequest(this.authConfig(), oauth2MaxKeyAuthSource);
    }
}
