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
package io.jpom.controller.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.SystemIpConfigModel;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ExtConfigBean;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 系统配置
 *
 * @author bwcx_jzy
 * @date 2019/08/08
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_CONFIG)
@SystemPermission
public class SystemConfigController extends BaseServerController {

	private final SystemParametersServer systemParametersServer;

	public SystemConfigController(SystemParametersServer systemParametersServer) {
		this.systemParametersServer = systemParametersServer;
	}

	/**
	 * get server's config or node's config
	 * 加载服务端或者节点端配置
	 *
	 * @param nodeId 节点ID
	 * @return json
	 * @throws IOException io
	 * @author Hotstrip
	 */
	@RequestMapping(value = "config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String configData(String nodeId) throws IOException {
		String content;
		if (StrUtil.isNotEmpty(nodeId)) {
			JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.SystemGetConfig, getRequest(), JSONObject.class);
			content = jsonObject.getString("content");
		} else {
			content = IoUtil.read(ExtConfigBean.getResource().getInputStream(), CharsetUtil.CHARSET_UTF_8);
		}
		return JsonMessage.getString(200, "加载成功", content);
	}

	@RequestMapping(value = "save_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String saveConfig(String nodeId, String content, String restart) {
		if (StrUtil.isNotEmpty(nodeId)) {
			return NodeForward.request(getNode(), getRequest(), NodeUrl.SystemSaveConfig).toString();
		}
		Assert.hasText(content, "内容不能为空");
		try {
			YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
			// @author hjk 前端编辑器允许使用tab键，并设定为2个空格，再转换为yml时要把tab键换成2个空格
			ByteArrayResource resource = new ByteArrayResource(content.replace("\t", "  ").getBytes(StandardCharsets.UTF_8));
			yamlPropertySourceLoader.load("test", resource);
		} catch (Exception e) {
			DefaultSystemLog.getLog().warn("内容格式错误，请检查修正", e);
			return JsonMessage.getString(500, "内容格式错误，请检查修正:" + e.getMessage());
		}
		Assert.state(!JpomManifest.getInstance().isDebug(), "调试模式下不支持在线修改,请到resources目录下的bin目录修改extConfig.yml");

		File resourceFile = ExtConfigBean.getResourceFile();
		FileUtil.writeString(content, resourceFile, CharsetUtil.CHARSET_UTF_8);

		if (Convert.toBool(restart, false)) {
			// 重启
			ThreadUtil.execute(() -> {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignored) {
				}
				JpomApplication.restart();
			});
		}
		return JsonMessage.getString(200, "修改成功");
	}


	/**
	 * 加载服务端的 ip 白名单配置
	 *
	 * @return json
	 */
	@RequestMapping(value = "ip-config-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.LIST)
	public String ipConfigData() {
		SystemIpConfigModel config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
		JSONObject jsonObject = new JSONObject();
		if (config != null) {
			jsonObject.put("allowed", config.getAllowed());
			jsonObject.put("prohibited", config.getProhibited());
		}
		//jsonObject.put("path", FileUtil.getAbsolutePath(systemIpConfigService.filePath()));
		jsonObject.put("ip", getIp());
		return JsonMessage.getString(200, "加载成功", jsonObject);
	}

	@RequestMapping(value = "save_ip_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.SYSTEM_CONFIG_IP, method = MethodFeature.EDIT)
	public String saveIpConfig(String allowed, String prohibited) {
		SystemIpConfigModel systemIpConfigModel = new SystemIpConfigModel();
		systemIpConfigModel.setAllowed(StrUtil.emptyToDefault(allowed, StrUtil.EMPTY));
		systemIpConfigModel.setProhibited(StrUtil.emptyToDefault(prohibited, StrUtil.EMPTY));
		systemParametersServer.upsert(SystemIpConfigModel.ID, systemIpConfigModel, SystemIpConfigModel.ID);
		//
		return JsonMessage.getString(200, "修改成功");
	}
}
