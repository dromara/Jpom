package io.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
public class NodeEditController extends BaseServerController {

	private final OutGivingServer outGivingServer;
	private final MonitorService monitorService;
	private final BuildInfoService buildService;
	private final ProjectInfoCacheService projectInfoCacheService;

	public NodeEditController(OutGivingServer outGivingServer,
							  MonitorService monitorService,
							  BuildInfoService buildService,
							  ProjectInfoCacheService projectInfoCacheService) {
		this.outGivingServer = outGivingServer;
		this.monitorService = monitorService;
		this.buildService = buildService;
		this.projectInfoCacheService = projectInfoCacheService;
	}


	@PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String listJson() {
		PageResultDto<NodeModel> nodeModelPageResultDto = nodeService.listPage(getRequest());
		return JsonMessage.getString(200, "", nodeModelPageResultDto);
	}

	@GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String listDataAll() {
		List<NodeModel> list = nodeService.listByWorkspace(getRequest());
		return JsonMessage.getString(200, "", list);
	}

	@RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String nodeStatus() {
		long timeMillis = System.currentTimeMillis();
		NodeModel node = getNode();
		JSONObject jsonObject = NodeForward.requestData(node, NodeUrl.Status, getRequest(), JSONObject.class);
		Assert.notNull(jsonObject, "获取信息失败");
		JSONArray jsonArray = new JSONArray();
		jsonObject.put("timeOut", System.currentTimeMillis() - timeMillis);
		jsonObject.put("nodeId", node.getId());
		jsonArray.add(jsonObject);
		return JsonMessage.getString(200, "", jsonArray);
	}

	@PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String save() {
		nodeService.update(getRequest(), false);
		return JsonMessage.getString(200, "操作成功");
	}


	/**
	 * 删除节点
	 *
	 * @param id 节点id
	 * @return json
	 */
	@PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String del(@ValidatorItem String id) {
		//  判断分发
		HttpServletRequest request = getRequest();
		boolean checkNode = outGivingServer.checkNode(id, request);
		Assert.state(!checkNode, "该节点存在分发项目，不能删除");
		// 监控
		boolean checkNode1 = monitorService.checkNode(id);
		Assert.state(!checkNode1, "该节点存在监控项，不能删除");
		boolean checkNode2 = buildService.checkNode(id);
		Assert.state(!checkNode2, "该节点存在构建项，不能删除");
		//
		ProjectInfoModel projectInfoModel = new ProjectInfoModel();
		projectInfoModel.setNodeId(id);
		projectInfoModel.setWorkspaceId(projectInfoCacheService.getCheckUserWorkspace(request));
		boolean exists = projectInfoCacheService.exists(projectInfoModel);
		Assert.state(!exists, "该节点下还存在项目，不能删除");
		nodeService.delByKey(id, request);
		return JsonMessage.getString(200, "操作成功");
	}

	@GetMapping(value = "un_lock_workspace", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	@SystemPermission(superUser = true)
	public String unLockWorkspace(@ValidatorItem String id, @ValidatorItem String workspaceId) {
		nodeService.unLock(id, workspaceId);
		return JsonMessage.getString(200, "操作成功");
	}
}
