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
import io.jpom.JpomApplication;
import io.jpom.common.*;
import io.jpom.system.AgentConfigBean;
import io.jpom.system.ConfigBean;
import org.springframework.http.MediaType;
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
public class SystemUpdateController extends BaseAgentController {

	@PostMapping(value = "uploadJar.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String uploadJar() throws IOException {
		//
		Objects.requireNonNull(JpomManifest.getScriptFile());
		MultipartFileBuilder multipartFileBuilder = createMultipart();
		String absolutePath = AgentConfigBean.getInstance().getTempPath().getAbsolutePath();
		multipartFileBuilder
				.setFileExt("jar", "zip")
				.addFieldName("file")
				.setUseOriginalFilename(true)
				.setSavePath(absolutePath);
		String path = multipartFileBuilder.save();
		// 解析压缩包
		File file = JpomManifest.zipFileFind(path, Type.Agent, absolutePath);
		path = FileUtil.getAbsolutePath(file);
		// 基础检查
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Agent);
		if (error.getCode() != HttpStatus.HTTP_OK) {
			return error.toString();
		}
		Tuple data = error.getData();
		String version = data.get(0);
		JpomManifest.releaseJar(path, version);
		//
		JpomApplication.restart();
		return JsonMessage.getString(200, Const.UPGRADE_MSG);
	}

	@PostMapping(value = "change_log", produces = MediaType.APPLICATION_JSON_VALUE)
	public String changeLog() {
		//
		URL resource = ResourceUtil.getResource("CHANGELOG.md");
		String log = StrUtil.EMPTY;
		if (resource != null) {
			InputStream stream = URLUtil.getStream(resource);
			log = IoUtil.readUtf8(stream);
		}
		return JsonMessage.getString(200, "", log);
	}

	/**
	 * 检查是否存在新版本
	 *
	 * @return json
	 * @see RemoteVersion
	 */
	@PostMapping(value = "check_version.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String checkVersion() {
		RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
		return JsonMessage.getString(200, "", remoteVersion);
	}

	/**
	 * 远程下载升级
	 *
	 * @return json
	 * @see RemoteVersion
	 */
	@PostMapping(value = "remote_upgrade.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String upgrade() throws IOException {
		RemoteVersion.upgrade(ConfigBean.getInstance().getTempPath().getAbsolutePath());
		return JsonMessage.getString(200, Const.UPGRADE_MSG);
	}
}
