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
package io.jpom.controller.outgiving;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.RegexPool;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.AgentWhitelist;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点白名单
 *
 * @author jiangzeyin
 * @since 2019/4/22
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING_CONFIG_WHITELIST)
public class OutGivingWhitelistController extends BaseServerController {

	private final SystemParametersServer systemParametersServer;
	private final OutGivingWhitelistService outGivingWhitelistService;

	public OutGivingWhitelistController(SystemParametersServer systemParametersServer,
										OutGivingWhitelistService outGivingWhitelistService) {
		this.systemParametersServer = systemParametersServer;
		this.outGivingWhitelistService = outGivingWhitelistService;
	}


	/**
	 * get whiteList data
	 * 白名单数据接口
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String whiteList() {
		ServerWhitelist serverWhitelist = outGivingWhitelistService.getServerWhitelistData(getRequest());
		Field[] fields = ReflectUtil.getFields(ServerWhitelist.class);
		Map<String, Object> map = new HashMap<>(8);
		for (Field field : fields) {
			Object value = ReflectUtil.getFieldValue(serverWhitelist, field);
			if (value instanceof Collection) {
				Collection<String> fieldValue = (Collection<String>) value;
				map.put(field.getName(), AgentWhitelist.convertToLine(fieldValue));
				map.put(field.getName() + "Array", fieldValue);
			}
		}
		return JsonMessage.getString(200, "ok", map);
	}


	/**
	 * 保存节点白名单
	 *
	 * @param outGiving 数据
	 * @return json
	 */
	@RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission
	@Feature(method = MethodFeature.EDIT)
	public String whitelistDirectorySubmit(String outGiving, String allowRemoteDownloadHost) {
		List<String> list = AgentWhitelist.parseToList(outGiving, true, "项目路径白名单不能为空");
		list = AgentWhitelist.covertToArray(list, "项目路径白名单不能位于Jpom目录下");

		ServerWhitelist serverWhitelist = outGivingWhitelistService.getServerWhitelistData(getRequest());
		serverWhitelist.setOutGiving(list);
		//
		List<String> allowRemoteDownloadHostList = AgentWhitelist.parseToList(allowRemoteDownloadHost, "运行远程下载的 host 不能配置为空");
		//
		if (CollUtil.isNotEmpty(allowRemoteDownloadHostList)) {
			for (String s : allowRemoteDownloadHostList) {
				Assert.state(ReUtil.isMatch(RegexPool.URL_HTTP, s), "配置的远程地址不规范,请重新填写：" + s);
			}
		}
		serverWhitelist.setAllowRemoteDownloadHost(allowRemoteDownloadHostList == null ? null : CollUtil.newHashSet(allowRemoteDownloadHostList));
		//
		String workspaceId = nodeService.getCheckUserWorkspace(getRequest());
		String id = ServerWhitelist.workspaceId(workspaceId);
		systemParametersServer.upsert(id, serverWhitelist, id);

		String resultData = AgentWhitelist.convertToLine(list);
		return JsonMessage.getString(200, "保存成功", resultData);
	}
}
