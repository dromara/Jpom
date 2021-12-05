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
package io.jpom.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JpomManifest;
import io.jpom.common.RemoteVersion;
import io.jpom.common.interceptor.NotAuthorize;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.util.JvmUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 首页
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@RestController
public class IndexController extends BaseAgentController {
	@Resource
	private WhitelistDirectoryService whitelistDirectoryService;
	@Resource
	private ProjectInfoService projectInfoService;

	@RequestMapping(value = {"index", "", "index.html", "/"}, produces = MediaType.TEXT_PLAIN_VALUE)
	@NotAuthorize
	public String index() {
		return "Jpom-Agent,Can't access directly,Please configure it to JPOM server";
	}

	@RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String info() {
		int code;
		if (whitelistDirectoryService.isInstalled()) {
			code = 200;
		} else {
			code = 201;
		}
		JpomManifest instance = JpomManifest.getInstance();
		RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
		//
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("manifest", instance);
		jsonObject.put("remoteVersion", remoteVersion);
		return JsonMessage.getString(code, "", jsonObject);
	}

	/**
	 * 返回节点项目状态信息
	 *
	 * @return array
	 */
	@RequestMapping(value = "status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String status() {
		List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("javaVirtualCount", JvmUtil.getJavaVirtualCount());
		jsonObject.put("osName", JpomManifest.getInstance().getOsName());
		jsonObject.put("jpomVersion", JpomManifest.getInstance().getVersion());
		jsonObject.put("javaVersion", SystemUtil.getJavaRuntimeInfo().getVersion());
		//  获取JVM中内存总大小
		long totalMemory = SystemUtil.getTotalMemory();
		jsonObject.put("totalMemory", FileUtil.readableFileSize(totalMemory));
		//
		long freeMemory = SystemUtil.getFreeMemory();
		jsonObject.put("freeMemory", FileUtil.readableFileSize(freeMemory));
		int count = 0;
		if (nodeProjectInfoModels != null) {
			count = nodeProjectInfoModels.size();
		}
		jsonObject.put("count", count);
		// 运行时间
		jsonObject.put("runTime", JpomManifest.getInstance().getUpTime());
		return JsonMessage.getString(200, "", jsonObject);
	}
}
