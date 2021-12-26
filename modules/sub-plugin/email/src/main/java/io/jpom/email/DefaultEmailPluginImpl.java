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
package io.jpom.email;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.plugin.DefaultPlugin;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;

import javax.mail.Session;
import javax.mail.Transport;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/12/22
 */
@PluginConfig(plugin = DefaultPlugin.Email)
public class DefaultEmailPluginImpl implements IDefaultPlugin {

	@Override
	public Object execute(Object main, Map<String, Object> parameter) throws Exception {
		MailAccount mailAccount = getAccount(main);
		//
		String toEmail = (String) parameter.get("toEmail");
		String title = (String) parameter.get("title");
		String context = (String) parameter.get("context");
		List<String> list = StrUtil.split(toEmail, StrUtil.COMMA, true, true);
		try {
			return MailUtil.send(mailAccount, list, title, context, false);
		} catch (cn.hutool.extra.mail.MailException mailException) {
			Exception cause = (Exception) mailException.getCause();
			if (cause != null) {
				throw cause;
			}
			throw mailException;
		}
	}

	@Override
	public boolean check(String type, Object main, Map<String, Object> parameter) {
		if (StrUtil.equals("checkInfo", type)) {
			try {
				MailAccount account = this.getAccount(main);
				Session session = MailUtil.getSession(account, false);
				Transport transport = session.getTransport("smtp");
				transport.connect();
				transport.close();
				return true;
			} catch (Exception e) {
				DefaultSystemLog.getLog().warn("检查邮箱信息错误：{}", e.getMessage());
				return false;
			}
		}
		throw new IllegalArgumentException("不支持的类型：" + type);
	}

	/**
	 * 创建邮件对象
	 *
	 * @param main 传人参数
	 * @return MailAccount
	 */
	private MailAccount getAccount(Object main) {
		if (!(main instanceof JSONObject)) {
			throw new IllegalArgumentException("插件端使用参数不正确");
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
		mailAccount.setTimeout(10 * 1000);
		mailAccount.setConnectionTimeout(10 * 1000);
		boolean sslEnable = data.getBooleanValue("sslEnable");
		//
		mailAccount.setSslEnable(sslEnable);
		//Integer socketFactoryPort = data.getInteger("socketFactoryPort");
//			if (socketFactoryPort != null) {
		mailAccount.setSocketFactoryPort(port);
//			}
		mailAccount.setAuth(true);
		return mailAccount;
	}
}
