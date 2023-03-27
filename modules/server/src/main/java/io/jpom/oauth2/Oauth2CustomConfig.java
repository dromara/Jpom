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
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2023/3/26
 */
@Data
public class Oauth2CustomConfig {

    private Boolean enabled;
    private String clientId;
    private String clientSecret;
    private String authorizationUri;
    private String accessTokenUri;
    private String userInfoUri;
    private String redirectUri;
    /**
     * 是否自动创建用户
     */
    private Boolean autoCreteUser;

    /**
     * 是否开启
     *
     * @return true 开启
     */
    public boolean enabled() {
        return enabled != null && enabled;
    }

    /**
     * 验证数据
     */
    public void check() {
        Assert.hasText(this.clientId, "没有配置 clientId");
        Assert.hasText(this.clientSecret, "没有配置 clientSecret");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.authorizationUri, "请配置正确的授权 url");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.accessTokenUri, "请配置正确的令牌 url");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.userInfoUri, "请配置正确的用户信息 url");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.redirectUri, "请配置正确的重定向 url");
    }
}
