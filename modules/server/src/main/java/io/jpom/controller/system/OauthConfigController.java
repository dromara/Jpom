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
package io.jpom.controller.system;

import io.jpom.common.JsonMessage;
import io.jpom.oauth2.Oauth2CustomAuthSource;
import io.jpom.oauth2.Oauth2CustomConfig;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author bwcx_jzy
 * @since 2023/3/26
 */
@RestController
@RequestMapping(value = "system/oauth-config")
@Feature(cls = ClassFeature.OAUTH_CONFIG)
@SystemPermission
public class OauthConfigController {

    private final SystemParametersServer systemParametersServer;
    private final Oauth2CustomAuthSource oauth2CustomAuthSource;

    public OauthConfigController(SystemParametersServer systemParametersServer,
                                 Oauth2CustomAuthSource oauth2CustomAuthSource) {
        this.systemParametersServer = systemParametersServer;
        this.oauth2CustomAuthSource = oauth2CustomAuthSource;
    }

    @GetMapping(value = "oauth2", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<Oauth2CustomConfig> oauth2() {
        Oauth2CustomConfig item = oauth2CustomAuthSource.getOauth2Config();
        return JsonMessage.success("", item);
    }

    @PostMapping(value = "oauth2-save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> saveOauth2(Oauth2CustomConfig oauth2CustomConfig) {
        Assert.notNull(oauth2CustomConfig, "请填写信息,并检查是否填写合法");
        if (oauth2CustomConfig.enabled()) {
            oauth2CustomConfig.check();
        }
        systemParametersServer.upsert(Oauth2CustomAuthSource.OAUTH_CONFIG_OAUTH2, oauth2CustomConfig, Oauth2CustomAuthSource.OAUTH_CONFIG_OAUTH2);
        //
        oauth2CustomAuthSource.refreshCache();
        return JsonMessage.success("保存成功");
    }
}
