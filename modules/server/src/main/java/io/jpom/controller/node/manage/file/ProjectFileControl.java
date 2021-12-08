package io.jpom.controller.node.manage.file;

import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 文件管理
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/node/manage/file/")
@Feature(cls = ClassFeature.PROJECT_FILE)
public class ProjectFileControl extends BaseServerController {

	private final ProjectInfoCacheService projectInfoService;

	public ProjectFileControl(ProjectInfoCacheService projectInfoService) {
		this.projectInfoService = projectInfoService;
	}

	/**
	 * 列出目录下的文件
	 *
	 * @return json
	 */
	@RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	// @ProjectPermission()
	@Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
	public String getFileList() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_GetFileList).toString();
	}


	/**
	 * 上传文件
	 *
	 * @return json
	 */
	@RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.UPLOAD)
	public String upload() {
		return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Manage_File_Upload).toString();
	}

	/**
	 * 下载文件
	 */
	@RequestMapping(value = "download", method = RequestMethod.GET)
	@ResponseBody
	@Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DOWNLOAD)
	public void download() {
		NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_File_Download);
	}

	/**
	 * 删除文件
	 *
	 * @return json
	 */
	@RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DEL)
	public String deleteFile() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_DeleteFile).toString();
	}


	/**
	 * 更新配置文件
	 *
	 * @return json
	 */
	@PostMapping(value = "update_config_file", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
	public String updateConfigFile() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_UpdateConfigFile).toString();
	}

	/**
	 * 删除文件
	 *
	 * @return json
	 */
	@GetMapping(value = "read_file", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
	public String readFile() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_ReadFile).toString();
	}

	/**
	 * 下载远程文件
	 *
	 * @return json
	 */
	@GetMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.REMOTE_DOWNLOAD)
	public String remoteDownload() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_Remote_Download).toString();

	}


//	/**
//	 * 获取可编辑文件格式
//	 *
//	 * @return json
//	 */
//	@RequestMapping(value = "geFileFormat", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
//	@ResponseBody
//	@Feature(method = MethodFeature.GET_FILE_FOMAT)
//	public String geFileFormat() {
//		String[] file = fileFormat.split("\\|");
//		JSONObject jsonObject = new JSONObject();
//		jsonObject.put("fileFormat", file);
//		return JsonMessage.getString(200, "获取成功", jsonObject);
//	}
}
