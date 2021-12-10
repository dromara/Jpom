package io.jpom.controller.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.RemoteVersion;
import io.jpom.common.Type;
import io.jpom.model.AgentFileModel;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/11/29
 */
@RestController
@RequestMapping(value = "/node")
@SystemPermission(superUser = true)
@Feature(cls = ClassFeature.UPGRADE_NODE_LIST)
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
		Tuple download = RemoteVersion.download(ConfigBean.getInstance().getTempPath().getAbsolutePath(), Type.Agent);
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
		AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class);
		JSONObject jsonObject = new JSONObject();
		if (remoteVersion == null) {
			jsonObject.put("upgrade", false);
		} else {
			String tagName = StrUtil.removePrefixIgnoreCase(remoteVersion.getTagName(), "v");
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
}
