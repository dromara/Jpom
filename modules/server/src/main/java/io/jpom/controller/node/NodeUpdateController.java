package io.jpom.controller.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.common.RemoteVersion;
import io.jpom.common.Type;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AgentFileModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.AgentFileService;
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
public class NodeUpdateController extends BaseServerController {

	private final AgentFileService agentFileService;

	public NodeUpdateController(AgentFileService agentFileService) {
		this.agentFileService = agentFileService;
	}

	/**
	 * 远程下载
	 *
	 * @return json
	 * @see RemoteVersion
	 */
	@GetMapping(value = "download_remote.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String downloadRemote() throws IOException {
		Tuple download = RemoteVersion.download(ConfigBean.getInstance().getTempPath().getAbsolutePath(), Type.Agent);
		// 保存文件
		this.saveAgentFile(download);
		return JsonMessage.getString(200, "下载成功");
	}

	@RequestMapping(value = "upload_agent", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.UpdateSys)
	@SystemPermission
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
		JsonMessage<Tuple> error = JpomManifest.checkJpomJar(path, Type.Agent.getApplicationClass(), false);
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
		// 保存Agent文件
		String id = Type.Agent.name().toLowerCase();

		AgentFileModel agentFileModel = agentFileService.getItem(id);
		if (agentFileModel == null) {
			agentFileModel = new AgentFileModel();
			agentFileModel.setId(id);
			agentFileService.addItem(agentFileModel);
		}
		agentFileModel.setName(file.getName());
		agentFileModel.setSize(file.length());
		agentFileModel.setSavePath(FileUtil.getAbsolutePath(file));
		//
		agentFileModel.setVersion(data.get(0));
		agentFileModel.setTimeStamp(data.get(1));
		agentFileService.updateItem(agentFileModel);
		// 删除历史包  @author jzy 2021-08-03
		String saveDir = ServerConfigBean.getInstance().getAgentPath().getAbsolutePath();
		List<File> files = FileUtil.loopFiles(saveDir, pathname -> !FileUtil.equals(pathname, file));
		for (File file1 : files) {
			FileUtil.del(file1);
		}
	}
}
