/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.email;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailException;
import cn.hutool.extra.mail.MailUtil;
import cn.keepbx.jpom.plugins.PluginConfig;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.plugin.IDefaultPlugin;

import javax.mail.Session;
import javax.mail.Transport;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/12/22
 */
@PluginConfig(name = "email")
@Slf4j
public class DefaultEmailPluginImpl implements IDefaultPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        if (main instanceof JSONObject) {
            MailAccount mailAccount = getAccount(main);
            //
            String toEmail = (String) parameter.get("toEmail");
            String title = (String) parameter.get("title");
            String context = (String) parameter.get("context");
            List<String> list = StrUtil.split(toEmail, StrUtil.COMMA, true, true);
            try {
                return MailUtil.send(mailAccount, list, title, context, false);
            } catch (MailException mailException) {
                Exception cause = (Exception) mailException.getCause();
                if (cause != null) {
                    throw cause;
                }
                throw mailException;
            }
        } else if (main instanceof String && StrUtil.equals("checkInfo", main.toString())) {
            try {
                Object data = parameter.get("data");
                MailAccount account = this.getAccount(data);
                Session session = MailUtil.getSession(account, false);
                try (Transport transport = session.getTransport("smtp")) {
                    transport.connect();
                }
                return true;
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.check_email_error.636c"), e.getMessage());
                return false;
            }
        }
        throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_type_with_colon2.7de2") + main);
    }

    /**
     * 创建邮件对象
     *
     * @param main 传人参数
     * @return MailAccount
     */
    private MailAccount getAccount(Object main) {
        if (!(main instanceof JSONObject)) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.plugin_parameter_incorrect.a355"));
        }
        JSONObject data = (JSONObject) main;
        MailAccount mailAccount = new MailAccount();
        String user = data.getString("user");
        String pass = data.getString("pass");
        String from = data.getString("from");
        Integer port = data.getInteger("port");
        String host = data.getString("host");
        mailAccount.setUser(user);
        mailAccount.setPass(pass);
        mailAccount.setFrom(from);
        mailAccount.setPort(port);
        mailAccount.setHost(host);
        //
        Integer timeout = data.getInteger("timeout");
        timeout = ObjectUtil.defaultIfNull(timeout, 10);
        timeout = Math.max(3, timeout);
        mailAccount.setTimeout(timeout * 1000);
        mailAccount.setConnectionTimeout(timeout * 1000);
        boolean sslEnable = data.getBooleanValue("sslEnable");
        //
        mailAccount.setSslEnable(sslEnable);
        //Integer socketFactoryPort = data.getInteger("socketFactoryPort");
//			if (socketFactoryPort != null) {
        if (sslEnable) {
            mailAccount.setSocketFactoryPort(port);
        }
//			}
        mailAccount.setAuth(true);
        return mailAccount;
    }
}
