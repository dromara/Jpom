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
package io.jpom.controller.docker;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.FileUtils;
import io.jpom.util.LogRecorder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
@RestController
@Feature(cls = ClassFeature.DOCKER)
@RequestMapping(value = "/docker/images")
public class DockerImagesController extends BaseServerController {

	private final DockerInfoService dockerInfoService;

	public DockerImagesController(DockerInfoService dockerInfoService) {
		this.dockerInfoService = dockerInfoService;
	}


	/**
	 * @return json
	 */
	@PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String list(@ValidatorItem String id) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("name", getParameter("name"));
		parameter.put("showAll", getParameter("showAll"));
		parameter.put("dangling", getParameter("dangling"));
		List<JSONObject> listContainer = (List<JSONObject>) plugin.execute("listImages", parameter);
		return JsonMessage.getString(200, "", listContainer);
	}


	/**
	 * @return json
	 */
	@GetMapping(value = "remove", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem String id, String imageId) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("imageId", imageId);
		plugin.execute("removeImage", parameter);
		return JsonMessage.getString(200, "执行成功");
	}

	/**
	 * @return json
	 */
	@GetMapping(value = "inspect", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String inspect(@ValidatorItem String id, String imageId) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("imageId", imageId);
		JSONObject inspectImage = (JSONObject) plugin.execute("inspectImage", parameter);
		return JsonMessage.getString(200, "", inspectImage);
	}

	/**
	 * @return json
	 */
	@GetMapping(value = "pull-image", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String pullImage(@ValidatorItem String id, String repository) throws Exception {
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id, getRequest());
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.put("repository", repository);
		//
		String uuid = IdUtil.fastSimpleUUID();
		File file = FileUtil.file(ServerConfigBean.getInstance().getUserTempPath(), "docker-log", uuid + ".log");
		LogRecorder logRecorder = LogRecorder.builder().file(file).build();
		logRecorder.info("start pull {}", repository);
		Consumer<String> logConsumer = logRecorder::info;
		parameter.put("logConsumer", logConsumer);
		ThreadUtil.execute(() -> {
			try {
				plugin.execute("pullImage", parameter);
			} catch (Exception e) {
				logRecorder.error("拉取异常", e);
			}
			logRecorder.info("pull end");
		});
		return JsonMessage.getString(200, "开始拉取", uuid);
	}

	/**
	 * 获取拉取的日志
	 *
	 * @param id   id
	 * @param line 需要获取的行号
	 * @return json
	 */
	@GetMapping(value = "pull-image-log", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getNowLog(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据") String id,
							@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "line") int line) {
		File file = FileUtil.file(ServerConfigBean.getInstance().getUserTempPath(), "docker-log", id + ".log");
		if (!file.exists()) {
			return JsonMessage.getString(201, "还没有日志文件");
		}
		JSONObject data = FileUtils.readLogFile(file, line);
		return JsonMessage.getString(200, "ok", data);
	}

	/**
	 * @return json
	 */
	@PostMapping(value = "create-container", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String createContainer(@RequestBody JSONObject jsonObject) throws Exception {
		Assert.hasText(jsonObject.getString("id"), "id 不能为空");
		Assert.hasText(jsonObject.getString("imageId"), "镜像不能为空");
		Assert.hasText(jsonObject.getString("name"), "容器名称不能为空");
		DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(jsonObject.getString("id"), getRequest());
		IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
		Map<String, Object> parameter = dockerInfoModel.toParameter();
		parameter.putAll(jsonObject);
		plugin.execute("createContainer", parameter);
		return JsonMessage.getString(200, "创建成功");
	}
}
