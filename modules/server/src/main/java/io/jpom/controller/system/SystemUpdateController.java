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
package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.*;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.dblog.BackupInfoService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @since 2019/7/22
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_UPGRADE)
@SystemPermission(superUser = true)
public class SystemUpdateController extends BaseServerController {

	private final BackupInfoService backupInfoService;

	public SystemUpdateController(BackupInfoService backupInfoService) {
		this.backupInfoService = backupInfoService;
	}

	@PostMapping(value = "info", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String info() {
		NodeModel nodeModel = tryGetNode();
		if (nodeModel != null) {
			return NodeForward.request(getNode(), getRequest(), NodeUrl.Info).toString();
		}
		JpomManifest instance = JpomManifest.getInstance();
		RemoteVersion remoteVersion = RemoteVersion.cacheInfo();
		//
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("manifest", instance);
		jsonObject.put("remoteVersion", remoteVersion);
		return JsonMessage.getString(200, "", jsonObject);
	}

	/**
	 * 更新日志
	 *
	 * @return changelog md
	 */
	@PostMapping(value = "change_log", produces = MediaType.APPLICATION_JSON_VALUE)
	public String changeLog() {
		NodeModel nodeModel = tryGetNode();
		if (nodeModel != null) {
			return NodeForward.request(getNode(), getRequest(), NodeUrl.CHANGE_LOG).toString();
		}
		//
		URL resource = ResourceUtil.getResource("CHANGELOG.md");
		String log = StrUtil.EMPTY;
		if (resource != null) {
			InputStream stream = URLUtil.getStream(resource);
			log = IoUtil.readUtf8(stream);
		}
		return JsonMessage.getString(200, "", log);
	}

	@PostMapping(value = "uploadJar.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String uploadJar() throws IOException {
		NodeModel nodeModel = tryGetNode();
		if (nodeModel != null) {
			return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.SystemUploadJar).toString();
		}
		//
		Objects.requireNonNull(JpomManifest.getScriptFile());
		MultipartFileBuilder multipartFileBuilder = createMultipart();
		String absolutePath = ServerConfigBean.getInstance().getUserTempPath().getAbsolutePath();
		multipartFileBuilder
				.setFileExt("jar", "zip")
				.addFieldName("file")
				.setUseOriginalFilename(true)
				.setSavePath(absolutePath);
		String path = multipartFileBuilder.save();
		// 解析压缩包
		File file = JpomManifest.zipFileFind(path, Type.Server, absolutePath);
		path = FileUtil.getAbsolutePath(file);
		// 基础检查
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Server);
		if (error.getCode() != HttpStatus.HTTP_OK) {
			return error.toString();
		}
		Tuple data = error.getData();
		String version = data.get(0);
		JpomManifest.releaseJar(path, version);
		//
		backupInfoService.autoBackup();
		//
		JpomApplication.restart();
		return JsonMessage.getString(200, Const.UPGRADE_MSG);
	}

	/**
	 * 检查是否存在新版本
	 *
	 * @return json
	 * @see RemoteVersion
	 */
	@PostMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String checkVersion() {
		NodeModel nodeModel = tryGetNode();
		if (nodeModel != null) {
			return NodeForward.request(getNode(), getRequest(), NodeUrl.CHECK_VERSION).toString();
		}
		RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
		return JsonMessage.getString(200, "", remoteVersion);
	}

	/**
	 * 远程下载升级
	 *
	 * @return json
	 * @see RemoteVersion
	 */
	@GetMapping(value = "remote_upgrade.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DOWNLOAD)
	public String upgrade() throws IOException {
		NodeModel nodeModel = tryGetNode();
		if (nodeModel != null) {
			return NodeForward.request(getNode(), getRequest(), NodeUrl.REMOTE_UPGRADE).toString();
		}
		RemoteVersion.upgrade(ConfigBean.getInstance().getTempPath().getAbsolutePath(), objects -> backupInfoService.autoBackup());
		return JsonMessage.getString(200, Const.UPGRADE_MSG);
	}
}
