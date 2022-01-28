package io.jpom.controller.node.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.OutGivingModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.node.ProjectInfoCacheModel;
import io.jpom.permission.NodeDataPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.OutGivingServer;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 项目管理
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class ProjectManageControl extends BaseServerController {

	private final OutGivingServer outGivingServer;
	private final MonitorService monitorService;
	private final BuildInfoService buildService;

	private final ProjectInfoCacheService projectInfoCacheService;

	public ProjectManageControl(OutGivingServer outGivingServer,
								MonitorService monitorService,
								BuildInfoService buildService,
								ProjectInfoCacheService projectInfoCacheService) {
		this.outGivingServer = outGivingServer;
		this.monitorService = monitorService;
		this.buildService = buildService;
		this.projectInfoCacheService = projectInfoCacheService;
	}


	/**
	 * 展示项目页面
	 */
	@RequestMapping(value = "project_copy_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String projectCopyList() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_ProjectCopyList).toString();
	}

	/**
	 * 获取正在运行的项目的端口和进程id
	 *
	 * @return json
	 */
	@RequestMapping(value = "getProjectPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getProjectPort() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectPort).toString();
	}

	/**
	 * 获取正在运行的项目的端口和进程id
	 *
	 * @return json
	 */
	@RequestMapping(value = "getProjectCopyPort", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getProjectCopyPort() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectCopyPort).toString();
	}


	/**
	 * 查询所有项目
	 *
	 * @return json
	 */
	@PostMapping(value = "get_project_info", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getProjectInfo() {
		PageResultDto<ProjectInfoCacheModel> modelPageResultDto = projectInfoCacheService.listPage(getRequest());
//		JSONArray jsonArray = projectInfoService.listAll(nodeModel, getRequest());
		return JsonMessage.getString(200, "", modelPageResultDto);
	}

	/**
	 * 删除项目
	 *
	 * @param id id
	 * @return json
	 */
	@PostMapping(value = "deleteProject", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String deleteProject(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id, String copyId) {
		NodeModel nodeModel = getNode();
		HttpServletRequest servletRequest = getRequest();
		if (StrUtil.isEmpty(copyId)) {
			// 检查节点分发
			List<OutGivingModel> outGivingModels = outGivingServer.listByWorkspace(servletRequest);
			if (outGivingModels != null) {
				boolean match = outGivingModels.stream().anyMatch(outGivingModel -> outGivingModel.checkContains(nodeModel.getId(), id));
				Assert.state(!match, "当前项目存在节点分发，不能直接删除");
//				for (OutGivingModel outGivingModel : outGivingModels) {
//					if (outGivingModel.checkContains(nodeModel.getId(), id)) {
//						return JsonMessage.getString(405, "当前项目存在节点分发，不能直接删除");
//					}
//				}
			}
			//
			List<MonitorModel> monitorModels = monitorService.listByWorkspace(servletRequest);
			if (monitorModels != null) {
				boolean match = monitorModels.stream().anyMatch(monitorModel -> monitorModel.checkNodeProject(nodeModel.getId(), id));
//				if (monitorService.checkProject(nodeModel.getId(), id)) {
//					return JsonMessage.getString(405, );
//				}
				Assert.state(!match, "当前项目存在监控项，不能直接删除");
			}

			boolean releaseMethod = buildService.checkReleaseMethod(nodeModel.getId() + StrUtil.COLON + id, servletRequest, BuildReleaseMethod.Project);
			Assert.state(!releaseMethod, "当前项目存在构建项，不能直接删除");
		}
		JsonMessage<Object> request = NodeForward.request(nodeModel, servletRequest, NodeUrl.Manage_DeleteProject);
		if (request.getCode() == HttpStatus.OK.value()) {
			//
			projectInfoCacheService.syncNode(nodeModel);
		}
		return request.toString();
	}

	/**
	 * 重启项目
	 * <p>
	 * nodeId,id,copyId
	 *
	 * @return json
	 */
	@RequestMapping(value = "restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String restart() {
		NodeModel nodeModel = getNode();
		return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Restart).toString();
	}


	/**
	 * 启动项目
	 * <p>
	 * nodeId,id,copyId
	 *
	 * @return json
	 */
	@RequestMapping(value = "start", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String start() {
		NodeModel nodeModel = getNode();
		return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Start).toString();
	}


	/**
	 * 关闭项目项目
	 * <p>
	 * nodeId,id,copyId
	 *
	 * @return json
	 */
	@RequestMapping(value = "stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EXECUTE)
	public String stop() {
		NodeModel nodeModel = getNode();
		return NodeForward.request(nodeModel, getRequest(), NodeUrl.Manage_Stop).toString();
	}
}
