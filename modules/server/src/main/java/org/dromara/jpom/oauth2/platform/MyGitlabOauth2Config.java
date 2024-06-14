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
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhyd.oauth.config.AuthDefaultSource;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.oauth2.BaseOauth2Config;
import org.dromara.jpom.oauth2.MyAuthGitlabRequest;
import org.springframework.util.Assert;

/**
 * 自建 Gitlab 配置
 *
 * @author bwcx_jzy
 * @see AuthDefaultSource#GITLAB
 * @since 2024/04/07
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MyGitlabOauth2Config extends BaseOauth2Config implements AuthSource {

    public static final String KEY = "OAUTH_CONFIG_MYGITLAB_OAUTH2";

    private String host;

    @Override
    public String provide() {
        return "mygitlab";
    }

    @Override
    public AuthRequest authRequest() {
        Assert.state(this.enabled(), StrUtil.format(I18nMessageUtil.get("i18n.oauth2_not_enabled.c8b7"), this.provide()));
        return new MyAuthGitlabRequest(this.authConfig(), this);
    }

    @Override
    public void check() {
        super.check();
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.host, I18nMessageUtil.get("i18n.configure_correct_self_hosted_gitlab_address.ad50"));
    }

    /**
     * @return str
     * @see AuthDefaultSource#GITLAB#authorize()
     */
    @Override
    public String authorize() {
        return UrlBuilder.of(this.host).addPath("/oauth/authorize").build();
    }

    @Override
    public String accessToken() {
        // return "https://gitlab.com/oauth/token";
        return UrlBuilder.of(this.host).addPath("/oauth/token").build();
    }

    @Override
    public String userInfo() {
        // "https://gitlab.com/api/v4/user";
        return UrlBuilder.of(this.host).addPath("/api/v4/user").build();
    }

    @Override
    public Class<? extends AuthDefaultRequest> getTargetClass() {
        return MyAuthGitlabRequest.class;
    }
}
