package io.jpom.controller.node.manage.log;

import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 控制台日志备份管理
 *
 * @author jiangzeyin
 * @date 2019/3/7
 */
@Controller
@RequestMapping(value = "node/manage/log")
@Feature(cls = ClassFeature.PROJECT)
public class LogBackController extends BaseServerController {

	private final ProjectInfoCacheService projectInfoCacheService;

	public LogBackController(ProjectInfoCacheService projectInfoCacheService) {
		this.projectInfoCacheService = projectInfoCacheService;
	}

	@RequestMapping(value = "export.html", method = RequestMethod.GET)
	@ResponseBody
	@Feature(method = MethodFeature.DOWNLOAD)
	public void export() {
		NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_Log_export);
	}

//    @RequestMapping(value = "logBack", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LOG)
//    public String console(String id, String copyId) {
//        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Manage_Log_logBack, getRequest(), JSONObject.class);
//        setAttribute("data", jsonObject);
//        //
//        setAttribute("copyId", copyId);
//        return "node/manage/logBack";
//    }

	/**
	 * @return
	 * @author Hotstrip
	 * get log back list
	 * 日志备份列表接口
	 */
	@RequestMapping(value = "log-back-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LOG)
	public String logBackList() {
		JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Manage_Log_logBack, getRequest(), JSONObject.class);
		return JsonMessage.getString(200, "success", jsonObject);
	}

	@RequestMapping(value = "logBack_download", method = RequestMethod.GET)
	@ResponseBody
	@Feature(method = MethodFeature.DOWNLOAD)
	public void download() {
		NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_Log_logBack_download);
	}

	@RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.DEL_FILE)
	public String clear() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Log_logBack_delete).toString();
	}

	@RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String logSize(String id, String copyId) {
		JSONObject info = projectInfoCacheService.getLogSize(getNode(), id, copyId);
		return JsonMessage.getString(200, "", info);
	}

	/**
	 * 重置日志
	 *
	 * @return json
	 */
	@RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String resetLog() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Log_ResetLog).toString();
	}
}
