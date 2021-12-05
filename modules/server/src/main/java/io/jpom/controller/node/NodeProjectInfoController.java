package io.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeProjectInfoController extends BaseServerController {

	private final ProjectInfoCacheService projectInfoCacheService;

	public NodeProjectInfoController(ProjectInfoCacheService projectInfoCacheService) {
		this.projectInfoCacheService = projectInfoCacheService;
	}

	/**
	 * @return json
	 * @author Hotstrip
	 * load node project list
	 * 加载节点项目列表
	 */
	@PostMapping(value = "node_project_list", produces = MediaType.APPLICATION_JSON_VALUE)
	public String nodeProjectList() {
		PageResultDto<ProjectInfoModel> resultDto = projectInfoCacheService.listPageNode(getRequest());
		return JsonMessage.getString(200, "success", resultDto);
	}


	/**
	 * load node project list
	 * 加载节点项目列表
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@PostMapping(value = "project_list", produces = MediaType.APPLICATION_JSON_VALUE)
	public String projectList() {
		PageResultDto<ProjectInfoModel> resultDto = projectInfoCacheService.listPage(getRequest());
		return JsonMessage.getString(200, "success", resultDto);
	}

	/**
	 * load node project list
	 * 加载节点项目列表
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@GetMapping(value = "project_list_all", produces = MediaType.APPLICATION_JSON_VALUE)
	public String projectListAll() {
		List<ProjectInfoModel> projectInfoModels = projectInfoCacheService.listByWorkspace(getRequest());
		return JsonMessage.getString(200, "", projectInfoModels);
	}
}
