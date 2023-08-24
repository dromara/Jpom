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
package org.dromara.jpom.controller.system;

import cn.hutool.core.lang.Tuple;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.oauth2.BaseOauth2Config;
import org.dromara.jpom.oauth2.Oauth2Factory;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

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

    public OauthConfigController(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    @GetMapping(value = "oauth2", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<BaseOauth2Config> oauth2(String provide) {
        Tuple tuple = BaseOauth2Config.getDbKey(provide);
        Assert.notNull(tuple, "没有对应的类型");
        BaseOauth2Config configDefNewInstance = systemParametersServer.getConfigDefNewInstance(tuple.get(0), tuple.get(1));
        return JsonMessage.success("", configDefNewInstance);
    }

    @PostMapping(value = "oauth2-save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> saveOauth2(HttpServletRequest request, String provide) {
        Tuple tuple = BaseOauth2Config.getDbKey(provide);
        Assert.notNull(tuple, "没有对应的类型");
        Class<BaseOauth2Config> oauth2ConfigClass = tuple.get(1);
        BaseOauth2Config oauth2Config = ServletUtil.toBean(request, oauth2ConfigClass, true);
        Assert.notNull(tuple, "没有对应的类型");
        if (oauth2Config.enabled()) {
            oauth2Config.check();
        }
        systemParametersServer.upsert(tuple.get(0), oauth2Config, oauth2Config.provide());
        //
        Oauth2Factory.put(oauth2Config);
        return JsonMessage.success("保存成功");
    }
}
