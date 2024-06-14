/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.lang.Tuple;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
        Assert.notNull(tuple, I18nMessageUtil.get("i18n.no_type.9153"));
        BaseOauth2Config configDefNewInstance = systemParametersServer.getConfigDefNewInstance(tuple.get(0), tuple.get(1));
        return JsonMessage.success("", configDefNewInstance);
    }

    @PostMapping(value = "oauth2-save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> saveOauth2(HttpServletRequest request, String provide) {
        Tuple tuple = BaseOauth2Config.getDbKey(provide);
        Assert.notNull(tuple, I18nMessageUtil.get("i18n.no_type.9153"));
        Class<BaseOauth2Config> oauth2ConfigClass = tuple.get(1);
        BaseOauth2Config oauth2Config = ServletUtil.toBean(request, oauth2ConfigClass, true);
        Assert.notNull(tuple, I18nMessageUtil.get("i18n.no_type.9153"));
        if (oauth2Config.enabled()) {
            oauth2Config.check();
        }
        systemParametersServer.upsert(tuple.get(0), oauth2Config, oauth2Config.provide());
        //
        Oauth2Factory.put(oauth2Config);
        return JsonMessage.success(I18nMessageUtil.get("i18n.save_succeeded.3b10"));
    }
}
