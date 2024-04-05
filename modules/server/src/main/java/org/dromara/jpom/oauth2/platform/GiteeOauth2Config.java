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

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.zhyd.oauth.request.AuthGiteeRequest;
import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.oauth2.BaseOauth2Config;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2023/3/30
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GiteeOauth2Config extends BaseOauth2Config {
    public static final String KEY = "OAUTH_CONFIG_GITEE_OAUTH2";

    @Override
    public String provide() {
        return "gitee";
    }

    @Override
    public AuthRequest authRequest() {
        Assert.state(this.enabled(), "没有开启此 " + this.provide() + " oauth2");
        return new AuthGiteeRequest(this.authConfig());
    }
}
