package io.jpom.controller.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import io.jpom.JpomAgentApplication;
import io.jpom.JpomApplication;
import io.jpom.common.BaseAgentController;
import io.jpom.common.JpomManifest;
import io.jpom.common.RemoteVersion;
import io.jpom.common.Type;
import io.jpom.system.AgentConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @date 2019/7/22
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
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, JpomAgentApplication.class);
		if (error.getCode() != HttpStatus.HTTP_OK) {
			return error.toString();
		}
		String version = error.getMsg();
		JpomManifest.releaseJar(path, version);
		//
		JpomApplication.restart();
		return JsonMessage.getString(200, "升级中大约需要30秒");
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
}
