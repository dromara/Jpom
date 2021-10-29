package io.jpom.controller.node.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.manage.ProjectInfoService;
import io.jpom.service.system.WhitelistDirectoryService;
import io.jpom.system.ConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目管理
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
@Controller
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
public class EditProjectController extends BaseServerController {
	@Resource
	private ProjectInfoService projectInfoService;
	@Resource
	private WhitelistDirectoryService whitelistDirectoryService;

	@RequestMapping(value = "getProjectData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getProjectData(@ValidatorItem String id) {
		JSONObject projectInfo = projectInfoService.getItem(getNode(), id);
		return JsonMessage.getString(200, "", projectInfo);
	}

	/**
	 * @return
	 * @author Hotstrip
	 * get project access list
	 * 获取项目的白名单
	 */
	@RequestMapping(value = "project-access-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String projectAccessList() {
		List<String> jsonArray = whitelistDirectoryService.getProjectDirectory(getNode());
		return JsonMessage.getString(200, "success", jsonArray);
	}

	/**
	 * 保存项目
	 *
	 * @param id id
	 * @return json
	 */
	@RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@OptLog(UserOperateLogV1.OptType.SaveProject)
	@Feature(method = MethodFeature.EDIT)
	public String saveProject(String id) {
		// 防止和Jpom冲突
		if (StrUtil.isNotEmpty(ConfigBean.getInstance().applicationTag) && ConfigBean.getInstance().applicationTag.equalsIgnoreCase(id)) {
			return JsonMessage.getString(401, "当前项目id已经被Jpom占用");
		}
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_SaveProject).toString();
	}


	/**
	 * 验证lib 暂时用情况
	 *
	 * @return json
	 */
	@RequestMapping(value = "judge_lib.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String saveProject() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Jude_Lib).toString();
	}
}
