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
package io.jpom.controller.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.*;
import io.jpom.common.interceptor.BaseJpomInterceptor;
import io.jpom.controller.openapi.NodeInfoController;
import io.jpom.model.AgentFileModel;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ServerConfigBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2021/11/29
 */
@RestController
@RequestMapping(value = "/node")
@SystemPermission(superUser = true)
@Feature(cls = ClassFeature.UPGRADE_NODE_LIST)
@Slf4j
public class NodeUpdateController extends BaseServerController {

	private final SystemParametersServer systemParametersServer;

	public NodeUpdateController(SystemParametersServer systemParametersServer) {
		this.systemParametersServer = systemParametersServer;
	}

	/**
	 * 远程下载
	 *
	 * @return json
	 * @see RemoteVersion
	 */
	@GetMapping(value = "download_remote.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.REMOTE_DOWNLOAD)
	public String downloadRemote() throws IOException {
		String saveDir = ServerConfigBean.getInstance().getAgentPath().getAbsolutePath();
		Tuple download = RemoteVersion.download(saveDir, Type.Agent, false);
		// 保存文件
		this.saveAgentFile(download);
		return JsonMessage.getString(200, "下载成功");
	}

	/**
	 * 检查版本更新
	 *
	 * @return json
	 * @see RemoteVersion
	 * @see AgentFileModel
	 */
	@GetMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String checkVersion() {
		RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
		AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class, agentFileModel1 -> {
			if (agentFileModel1 == null || !FileUtil.exist(agentFileModel1.getSavePath())) {
				return null;
			}
			return agentFileModel1;
		});
		JSONObject jsonObject = new JSONObject();
		if (remoteVersion == null) {
			jsonObject.put("upgrade", false);
		} else {
			String tagName = StrUtil.removePrefixIgnoreCase(remoteVersion.getTagName(), "v");
			jsonObject.put("tagName", tagName);
			if (agentFileModel == null) {
				jsonObject.put("upgrade", true);
			} else {
				String version = StrUtil.removePrefixIgnoreCase(agentFileModel.getVersion(), "v");
				jsonObject.put("upgrade", StrUtil.compareVersion(version, tagName) < 0);
			}
		}
		return JsonMessage.getString(200, "", jsonObject);
	}

	@RequestMapping(value = "upload_agent", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission
	@Feature(method = MethodFeature.UPLOAD)
	public String uploadAgent() throws IOException {
		String saveDir = ServerConfigBean.getInstance().getAgentPath().getAbsolutePath();
		MultipartFileBuilder multipartFileBuilder = createMultipart();
		multipartFileBuilder
				.setFileExt("jar", "zip")
				.addFieldName("file")
				.setUseOriginalFilename(true)
				.setSavePath(saveDir);
		String path = multipartFileBuilder.save();
		// 解析压缩包
		File file = JpomManifest.zipFileFind(path, Type.Agent, saveDir);
		path = FileUtil.getAbsolutePath(file);
		// 基础检查
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Agent, false);
		if (error.getCode() != HttpStatus.HTTP_OK) {
			FileUtil.del(path);
			return error.toString();
		}
		// 保存文件
		this.saveAgentFile(error.getData());
		return JsonMessage.getString(200, "上传成功");
	}

	private void saveAgentFile(Tuple data) {
		File file = data.get(3);
		AgentFileModel agentFileModel = new AgentFileModel();
		agentFileModel.setName(file.getName());
		agentFileModel.setSize(file.length());
		agentFileModel.setSavePath(FileUtil.getAbsolutePath(file));
		//
		agentFileModel.setVersion(data.get(0));
		agentFileModel.setTimeStamp(data.get(1));
		systemParametersServer.upsert(AgentFileModel.ID, agentFileModel, AgentFileModel.ID);
		// 删除历史包  @author jzy 2021-08-03
		String saveDir = ServerConfigBean.getInstance().getAgentPath().getAbsolutePath();
		List<File> files = FileUtil.loopFiles(saveDir, pathname -> !FileUtil.equals(pathname, file));
		for (File file1 : files) {
			FileUtil.del(file1);
		}
	}

	@GetMapping(value = "fast_install.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String fastInstall() {
		InputStream inputStream = ResourceUtil.getStream("classpath:/fast-install-info.json");
		String json = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
		JSONObject jsonObject = new JSONObject();
		JpomManifest instance = JpomManifest.getInstance();
		jsonObject.put("token", instance.randomIdSign());
		jsonObject.put("key", ServerOpenApi.PUSH_NODE_KEY);
		//
		JSONArray jsonArray = JSONArray.parseArray(json);
		jsonObject.put("shUrls", jsonArray);
		//
		String contextPath = UrlRedirectUtil.getHeaderProxyPath(getRequest(), BaseJpomInterceptor.PROXY_PATH);
		String url = String.format("/%s/%s", contextPath, ServerOpenApi.RECEIVE_PUSH);
		jsonObject.put("url", FileUtil.normalize(url));
		return JsonMessage.getString(200, "", jsonObject);
	}

	@GetMapping(value = "pull_fast_install_result.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String pullFastInstallResult(String removeId) {
		Collection<JSONObject> jsonObjects = NodeInfoController.listReceiveCache(removeId);
		jsonObjects = jsonObjects.stream().map(jsonObject -> {
			JSONObject clone = jsonObject.clone();
			clone.remove("canUseNode");
			return clone;
		}).collect(Collectors.toList());
		return JsonMessage.getString(200, "", jsonObjects);
	}

	@GetMapping(value = "confirm_fast_install.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String confirmFastInstall(@ValidatorItem String id, @ValidatorItem String ip, int port) {
		JSONObject receiveCache = NodeInfoController.getReceiveCache(id);
		Assert.notNull(receiveCache, "没有对应的缓存信息");
		JSONArray jsonArray = receiveCache.getJSONArray("canUseNode");
		Assert.notEmpty(jsonArray, "没有对应的缓存信息：-1");
		Optional<NodeModel> any = jsonArray.stream().map(o -> {
			if (o instanceof NodeModel) {
				return (NodeModel) o;
			}
			JSONObject jsonObject = (JSONObject) o;
			return jsonObject.toJavaObject(NodeModel.class);
		}).filter(nodeModel -> StrUtil.equals(nodeModel.getUrl(), StrUtil.format("{}:{}", ip, port))).findAny();
		Assert.state(any.isPresent(), "ip 地址信息不正确");
		NodeModel nodeModel = any.get();
		try {
			nodeService.testNode(nodeModel);
		} catch (Exception e) {
			log.warn("测试结果：{} {}", nodeModel.getUrl(), e.getMessage());
			return JsonMessage.getString(500, "节点连接失败：" + e.getMessage());
		}
		// 插入
		boolean exists = nodeService.existsByUrl(nodeModel.getUrl(), nodeModel.getWorkspaceId(), null);
		Assert.state(!exists, "对应的节点已经存在拉：" + nodeModel.getUrl());
		nodeService.insert(nodeModel);
		// 更新结果
		receiveCache.put("type", "success");
		return JsonMessage.getString(200, "安装成功", NodeInfoController.listReceiveCache(null));
	}


}
