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

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.oauth2.custom.TopiamAuthOauth2Request;
import org.dromara.jpom.oauth2.custom.TopiamOauth2AuthSource;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/12/03
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TopiamOauth2Config extends CustomOauth2Config {
    public static final String KEY = "OAUTH_CONFIG_TOPIAM_OAUTH2";


    @Override
    public String provide() {
        return "topiam";
    }

    public AuthRequest authRequest() {
        Assert.state(this.enabled(), StrUtil.format(I18nMessageUtil.get("i18n.oauth2_not_enabled.c8b7"), this.provide()));
        TopiamOauth2AuthSource oauth2AuthSource = new TopiamOauth2AuthSource(this);
        AuthConfig config = this.authConfig();
        List<String> scopes = new ArrayList<>();
        scopes.add("openid");
        scopes.add("email");
        scopes.add("profile");
        config.setScopes(scopes);
        return new TopiamAuthOauth2Request(config, oauth2AuthSource);
    }
}
