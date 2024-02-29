/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.monitor;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSON;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.model.data.MailAccountModel;
import org.dromara.jpom.monitor.EmailUtil;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控邮箱配置
 *
 * @author bwcx_jzy
 * @since 2019/7/16
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_EMAIL)
@SystemPermission
public class SystemMailConfigController extends BaseServerController {

    private final SystemParametersServer systemParametersServer;

    public SystemMailConfigController(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * load mail config data
     * 加载邮件配置
     *
     * @return json
     * @author Hotstrip
     */
    @PostMapping(value = "mail-config-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<MailAccountModel> mailConfigData() {
        MailAccountModel item = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
        if (item != null) {
            item.setPass(null);
        }
        return JsonMessage.success("success", item);
    }

    @PostMapping(value = "mailConfig_save.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> listData(MailAccountModel mailAccountModel) throws Exception {
        Assert.notNull(mailAccountModel, "请填写信息,并检查是否填写合法");
        Assert.hasText(mailAccountModel.getHost(), "请填写host");
        Assert.hasText(mailAccountModel.getUser(), "请填写user");
        Assert.hasText(mailAccountModel.getFrom(), "请填写from");
        // 验证是否正确
        MailAccountModel item = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
        if (item != null) {
            mailAccountModel.setPass(StrUtil.emptyToDefault(mailAccountModel.getPass(), item.getPass()));
        } else {
            Assert.hasText(mailAccountModel.getPass(), "请填写pass");
        }
        IPlugin plugin = PluginFactory.getPlugin("email");
        Object json = JSON.toJSON(mailAccountModel);
        Map<String, Object> map = new HashMap<>(1);
        map.put("data", json);
        boolean checkInfo = plugin.execute("checkInfo", map, Boolean.class);
        Assert.state(checkInfo, "验证邮箱信息失败，请检查配置的邮箱信息。端口号、授权码等。");
        systemParametersServer.upsert(MailAccountModel.ID, mailAccountModel, MailAccountModel.ID);
        //
        EmailUtil.refreshConfig();
        return JsonMessage.success("保存成功");
    }
}
