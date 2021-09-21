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
import io.jpom.JpomServerApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.RemoteVersion;
import io.jpom.common.Type;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.system.ServerConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @date 2019/7/22
 */
@RestController
@RequestMapping(value = "system")
public class SystemUpdateController extends BaseServerController {

	@RequestMapping(value = "info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@SystemPermission
	public String info() {
		NodeModel nodeModel = tryGetNode();
		if (nodeModel != null) {
			return NodeForward.request(getNode(), getRequest(), NodeUrl.Info).toString();
		}
		return JsonMessage.getString(200, "", JpomManifest.getInstance());
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

	@RequestMapping(value = "uploadJar.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.UpdateSys)
	@SystemPermission
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
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, JpomServerApplication.class);
		if (error.getCode() != HttpStatus.HTTP_OK) {
			return error.toString();
		}
		Tuple data = error.getData();
		String version = data.get(0);
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
		NodeModel nodeModel = tryGetNode();
		if (nodeModel != null) {
			return NodeForward.request(getNode(), getRequest(), NodeUrl.CHECK_VERSION).toString();
		}
		RemoteVersion remoteVersion = RemoteVersion.loadRemoteInfo();
		return JsonMessage.getString(200, "", remoteVersion);
	}
}
