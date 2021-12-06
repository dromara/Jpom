package io.jpom.controller.node.tomcat;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.tomcat.TomcatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * tomcat 管理
 *
 * @author lf
 */
@RestController
@RequestMapping(value = TomcatManageController.TOMCAT_URL)
@Feature(cls = ClassFeature.TOMCAT)
public class TomcatManageController extends BaseServerController {

	public static final String TOMCAT_URL = "/node/tomcat/";


	@Resource
	private TomcatService tomcatService;

	/**
	 * @return
	 * @Hotstrip get tomcat list
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String tomcatList() {
		JSONArray jsonArray = tomcatService.getTomcatList(getNode());
		return JsonMessage.getString(200, "success", jsonArray);
	}

	/**
	 * 查询tomcat的项目
	 *
	 * @param id id
	 * @return tomcat的项目信息
	 */
	@RequestMapping(value = "getTomcatProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getTomcatProject(String id) {
		// 查询tomcat管理的项目的列表
		JSONArray tomcatProjects = tomcatService.getTomcatProjectList(getNode(), id);
		return JsonMessage.getString(200, "查询成功", tomcatProjects);
	}

	/**
	 * 查询tomcat状态
	 *
	 * @return tomcat运行状态
	 */
	@RequestMapping(value = "getTomcatStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getStatus() {
		return tomcatService.getTomcatStatus(getNode(), getRequest());
	}


	/**
	 * 保存Tomcat信息
	 *
	 * @param id tomcat的id,如果id非空则更新，如果id是空则保存
	 * @return 操作结果
	 */
	@RequestMapping(value = "save", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String save(String id) {
		NodeModel nodeModel = getNode();
		if (StrUtil.isEmpty(id)) {
			return tomcatService.addTomcat(nodeModel, getRequest());
		} else {
			// 修改Tomcat信息
			return tomcatService.updateTomcat(nodeModel, getRequest());
		}
	}

	/**
	 * 删除tomcat
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String delete() {
		return tomcatService.delete(getNode(), getRequest());
	}


	/**
	 * tomcat项目管理
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "tomcatProjectManage", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String tomcatProjectManage() {
		return tomcatService.tomcatProjectManage(getNode(), getRequest());
	}

	/**
	 * 启动tomcat
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String start() {
		return tomcatService.start(getNode(), getRequest());
	}

	/**
	 * 重启tomcat
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String restart() {
		return tomcatService.restart(getNode(), getRequest());
	}

	/**
	 * 停止tomcat
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String stop() {
		return tomcatService.stop(getNode(), getRequest());
	}


	/**
	 * 查询文件列表
	 *
	 * @return 文件列表
	 */
	@RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.TEXT_HTML_VALUE)
	@Feature(cls = ClassFeature.TOMCAT_FILE, method = MethodFeature.LIST)
	public String getFileList() {
		return tomcatService.getFileList(getNode(), getRequest());
	}


	/**
	 * 上传文件
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.TOMCAT_FILE, method = MethodFeature.UPLOAD)
	public String upload() {
		return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Tomcat_File_Upload).toString();
	}

	/**
	 * 上传War包
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "uploadWar", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.TOMCAT_FILE, method = MethodFeature.UPLOAD)
	public String uploadWar() {
		return tomcatService.uploadWar(getNode(), getMultiRequest());
	}

	/**
	 * 下载文件
	 */
	@RequestMapping(value = "download", method = RequestMethod.GET)
	@Feature(cls = ClassFeature.TOMCAT_FILE, method = MethodFeature.DOWNLOAD)
	public void download() {
		tomcatService.download(getNode(), getRequest(), getResponse());
	}

	/**
	 * 删除文件
	 *
	 * @return 操作结果
	 */
	@RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(cls = ClassFeature.TOMCAT_FILE, method = MethodFeature.DEL)
	public String deleteFile() {
		return tomcatService.deleteFile(getNode(), getRequest());
	}
}
