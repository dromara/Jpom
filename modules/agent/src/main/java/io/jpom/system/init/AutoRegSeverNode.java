/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.system.init;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.AgentConfigBean;
import io.jpom.system.AgentExtConfigBean;
import io.jpom.system.ConfigBean;
import io.jpom.util.JsonFileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 * 自动注册server 节点
 *
 * @author bwcx_jzy
 * @date 2019/8/6
 */
@PreLoadClass
public class AutoRegSeverNode {

	@PreLoadMethod
	private static void reg() throws FileNotFoundException {
		AgentExtConfigBean instance = AgentExtConfigBean.getInstance();
		String agentId = instance.getAgentId();
		String serverUrl = instance.getServerUrl();
		if (StrUtil.isEmpty(agentId) || StrUtil.isEmpty(serverUrl)) {
			//  如果二者缺一不注册
			return;
		}
		String oldInstallId = null;
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), AgentConfigBean.SERVER_ID);
		JSONObject serverJson = null;
		if (file.exists()) {
			serverJson = (JSONObject) JsonFileUtil.readJson(file.getAbsolutePath());
			oldInstallId = serverJson.getString("installId");
		}
		HttpRequest installRequest = instance.createServerRequest(ServerOpenApi.INSTALL_ID);
		String body1 = installRequest.execute().body();
		JsonMessage jsonMessage = JSON.parseObject(body1, JsonMessage.class);
		if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
			DefaultSystemLog.getLog().error("获取Server 安装id失败:" + jsonMessage);
			return;
		}
		String installId = jsonMessage.dataToString();
		boolean eqInstall = StrUtil.equals(oldInstallId, installId);
		//
		URL url = URLUtil.toUrlForHttp(instance.getAgentUrl());
		String protocol = url.getProtocol();

		HttpRequest serverRequest = instance.createServerRequest(ServerOpenApi.UPDATE_NODE_INFO);
		serverRequest.form("id", agentId);
		serverRequest.form("name", "节点：" + agentId);
		serverRequest.form("openStatus", 1);
		serverRequest.form("protocol", protocol);
		serverRequest.form("url", url.getHost() + ":" + url.getPort());
		serverRequest.form("loginName", AgentAuthorize.getInstance().getAgentName());
		serverRequest.form("loginPwd", AgentAuthorize.getInstance().getAgentPwd());
		serverRequest.form("type", eqInstall ? "update" : "add");
		String body = serverRequest.execute().body();
		DefaultSystemLog.getLog().info("自动注册Server:" + body);
		JsonMessage regJsonMessage = JSON.parseObject(body, JsonMessage.class);
		if (regJsonMessage.getCode() == HttpStatus.HTTP_OK) {
			if (serverJson == null) {
				serverJson = new JSONObject();
			}
			if (!eqInstall) {
				serverJson.put("installId", installId);
				serverJson.put("regTime", DateTime.now().toString());
			} else {
				serverJson.put("updateTime", DateTime.now().toString());
			}
			JsonFileUtil.saveJson(file.getAbsolutePath(), serverJson);
		} else {
			DefaultSystemLog.getLog().error("自动注册插件端失败：{}", body);
		}
	}
}
