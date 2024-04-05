/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.oauth2.platform;

import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.lang.Validator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.oauth2.AuthOauth2MaxKeyRequest;
import org.dromara.jpom.oauth2.BaseOauth2Config;
import org.dromara.jpom.oauth2.Oauth2MaxKeyAuthSource;
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
