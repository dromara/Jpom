/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.monitor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.MailAccountModel;
import org.dromara.jpom.model.data.MonitorModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.system.SystemParametersServer;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件工具
 *
 * @author Arno
 */
@Slf4j
public class EmailUtil implements INotify {

    private static SystemParametersServer systemParametersServer;
    private static MailAccountModel config;

    @Override
    public void send(MonitorModel.Notify notify, String title, String context) throws Exception {
        String value = notify.getValue();
        EmailUtil.send(value, title, context);
    }

    private static void init() {
        if (systemParametersServer == null) {
            systemParametersServer = SpringUtil.getBean(SystemParametersServer.class);
        }
    }

    /**
     * 加载配置信息
     */
    public static void refreshConfig() {
        if (config == null) {
            init();
        }
        config = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
    }


    /**
     * 发送邮箱
     *
     * @param email   收件人
     * @param title   标题
     * @param context 内容
     */
    public static void send(String email, String title, String context) throws Exception {
        if (config == null) {
            // 没有数据才加载
            refreshConfig();
        }
        if (config == null || StrUtil.isEmpty(config.getHost())) {
            log.error(I18nMessageUtil.get("i18n.email_service_not_configured.3180"), email, title);
            return;
        }
        //
        Map<String, Object> mailMap = new HashMap<>(10);
        mailMap.put("toEmail", email);
        mailMap.put("title", title);
        mailMap.put("context", context);
        //
        IPlugin plugin = PluginFactory.getPlugin("email");
        plugin.execute(JSON.toJSON(config), mailMap);
    }
}
