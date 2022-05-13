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
package io.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.system.AgentAutoUser;
import io.jpom.util.JsonFileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * agent 端授权账号信息
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
@Configuration
public class AgentAuthorize {

	private static AgentAuthorize agentAuthorize;
	/**
	 * 账号
	 */
	@Value("${" + ConfigBean.AUTHORIZE_USER_KEY + "}")
	private String agentName;
	/**
	 * 密码
	 */
	@Value("${" + ConfigBean.AUTHORIZE_AUTHORIZE_KEY + ":}")
	private String agentPwd;
	/**
	 * 授权加密字符串
	 */
	private String authorize;

	/**
	 * 单例
	 *
	 * @return this
	 */
	public static AgentAuthorize getInstance() {
		if (agentAuthorize == null) {
			agentAuthorize = SpringUtil.getBean(AgentAuthorize.class);
			// 登录名不能为空
			if (StrUtil.isEmpty(agentAuthorize.agentName)) {
				throw new JpomRuntimeException("The agent login name cannot be empty");
			}
			agentAuthorize.checkPwd();
			// 生成密码授权字符串
			agentAuthorize.authorize = SecureUtil.sha1(agentAuthorize.agentName + "@" + agentAuthorize.agentPwd);
		}
		return agentAuthorize;
	}

	public String getAgentName() {
		return agentName;
	}

	public String getAgentPwd() {
		return agentPwd;
	}

	/**
	 * 判断授权是否正确
	 *
	 * @param authorize 授权
	 * @return true 正确
	 */
	public boolean checkAuthorize(String authorize) {
		return StrUtil.equals(authorize, this.authorize);
	}

	/**
	 * 检查是否配置密码
	 */
	private void checkPwd() {
		String path = ConfigBean.getInstance().getAgentAutoAuthorizeFile(ConfigBean.getInstance().getDataPath());
		if (StrUtil.isNotEmpty(agentPwd)) {
			// 有指定密码 清除旧密码信息
			FileUtil.del(path);
			Console.log("Authorization information has been customized,account：{}", this.agentName);
			return;
		}
		if (FileUtil.exist(path)) {
			// 读取旧密码
			try {
				String json = FileUtil.readString(path, CharsetUtil.CHARSET_UTF_8);
				AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
				if (!StrUtil.equals(autoUser.getAgentName(), this.agentName)) {
					throw new JpomRuntimeException("The existing login name is inconsistent with the configured login name");
				}
				String oldAgentPwd = autoUser.getAgentPwd();
				if (StrUtil.isNotEmpty(oldAgentPwd)) {
					this.agentPwd = oldAgentPwd;
					Console.log("Already authorized account:{}  password:{}  Authorization information storage location：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
					return;
				}
			} catch (JpomRuntimeException e) {
				throw e;
			} catch (Exception ignored) {
			}
		}
		this.agentPwd = RandomUtil.randomString(10);
		AgentAutoUser autoUser = new AgentAutoUser();
		autoUser.setAgentName(this.agentName);
		autoUser.setAgentPwd(this.agentPwd);
		// 写入文件中
		JsonFileUtil.saveJson(path, autoUser.toJson());
		Console.log("Automatically generate authorized account:{}  password:{}  Authorization information storage location：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
	}
}
