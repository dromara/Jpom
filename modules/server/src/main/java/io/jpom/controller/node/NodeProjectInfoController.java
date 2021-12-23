package io.jpom.controller.node;

import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
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

	/**
	 * 同步节点项目
	 *
	 * @return json
	 */
	@GetMapping(value = "sync_project", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
	public String syncProject(String nodeId) {
		NodeModel nodeModel = nodeService.getByKey(nodeId);
		Assert.notNull(nodeModel, "对应的节点不存在");
		String msg = projectInfoCacheService.syncExecuteNode(nodeModel);
		return JsonMessage.getString(200, msg);
	}

	/**
	 * 删除节点缓存的项目信息
	 *
	 * @return json
	 */
	@GetMapping(value = "del_project_cache", produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission()
	@Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
	public String delProjectCache(String nodeId) {
		NodeModel nodeModel = nodeService.getByKey(nodeId);
		Assert.notNull(nodeModel, "对应的节点不存在");
		int count = projectInfoCacheService.delCache(nodeId, getRequest());
		return JsonMessage.getString(200, "成功删除" + count + "条项目缓存");
	}

	/**
	 * 删除节点缓存的所有项目
	 *
	 * @return json
	 */
	@GetMapping(value = "clear_all_project", produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission(superUser = true)
	@Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
	public String clearAll() {
		Entity where = Entity.create();
		where.set("id", " <> id");
		int del = projectInfoCacheService.del(where);
		return JsonMessage.getString(200, "成功删除" + del + "条项目缓存");
	}


}
