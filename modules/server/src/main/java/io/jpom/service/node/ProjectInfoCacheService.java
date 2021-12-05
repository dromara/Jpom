package io.jpom.service.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@Service
public class ProjectInfoCacheService extends BaseNodeService<ProjectInfoModel> {

	private final NodeService nodeService;
	private final WorkspaceService workspaceService;

	public ProjectInfoCacheService(NodeService nodeService,
								   WorkspaceService workspaceService) {
		this.nodeService = nodeService;
		this.workspaceService = workspaceService;
	}

	/**
	 * 查询远端项目
	 *
	 * @param nodeModel 节点ID
	 * @param id        项目ID
	 * @return json
	 */
	public JSONObject getItem(NodeModel nodeModel, String id) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectItem, JSONObject.class, "id", id);
	}

	public JSONObject getLogSize(NodeModel nodeModel, String id, String copyId) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_Log_LogSize, JSONObject.class, "id", id, "copyId", copyId);
	}

	/**
	 * 同步所有节点的项目
	 */
	public void syncAllNode() {
		ThreadUtil.execute(() -> {
			List<NodeModel> list = nodeService.list();
			if (CollUtil.isEmpty(list)) {
				return;
			}
			for (NodeModel nodeModel : list) {
				this.syncNode(nodeModel);
			}
		});
	}

	/**
	 * 同步节点的项目
	 *
	 * @param nodeModel 节点
	 */
	public void syncNode(final NodeModel nodeModel) {
		if (!nodeModel.isOpenStatus()) {
			return;
		}
		ThreadUtil.execute(() -> {
			try {
				JSONArray jsonArray = NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectInfo, JSONArray.class, "notStatus", "true");
				if (CollUtil.isEmpty(jsonArray)) {
					return;
				}
				// 查询现在存在的项目
				ProjectInfoModel where = new ProjectInfoModel();
				where.setWorkspaceId(nodeModel.getWorkspaceId());
				where.setNodeId(nodeModel.getId());
				List<ProjectInfoModel> projectInfoModelsCacheAll = super.listByBean(where);
				projectInfoModelsCacheAll = ObjectUtil.defaultIfNull(projectInfoModelsCacheAll, Collections.EMPTY_LIST);
				Set<String> cacheIds = projectInfoModelsCacheAll.stream()
						.map(ProjectInfoModel::getProjectId)
						.collect(Collectors.toSet());
				//
				List<ProjectInfoModel> projectInfoModels = jsonArray.toJavaList(ProjectInfoModel.class);
				List<ProjectInfoModel> models = projectInfoModels.stream().peek(projectInfoModel -> {
					projectInfoModel.setProjectId(projectInfoModel.getId());
					projectInfoModel.setId(null);
					projectInfoModel.setNodeId(nodeModel.getId());
					if (StrUtil.isEmpty(projectInfoModel.getWorkspaceId())) {
						projectInfoModel.setWorkspaceId(nodeModel.getWorkspaceId());
					}
					projectInfoModel.setId(projectInfoModel.fullId());
				}).filter(projectInfoModel -> {
					// 检查对应的工作空间 是否存在
					return workspaceService.exists(new WorkspaceModel(projectInfoModel.getWorkspaceId()));
				}).peek(projectInfoModel -> cacheIds.remove(projectInfoModel.getProjectId())).collect(Collectors.toList());
				// 设置 临时缓存，便于放行检查
				BaseServerController.resetInfo(UserModel.EMPTY);
				//
				models.forEach(ProjectInfoCacheService.super::upsert);
				// 删除项目
				Set<String> strings = cacheIds.stream()
						.map(s -> ProjectInfoModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), s))
						.collect(Collectors.toSet());
				if (CollUtil.isNotEmpty(strings)) {
					super.delByKey(strings, null);
				}
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("同步节点项目失败:" + nodeModel.getId(), e);
			} finally {
				BaseServerController.remove();
			}
		});
	}

	/**
	 * 同步节点的项目
	 *
	 * @param nodeModel 节点
	 */
	public void syncNode(final NodeModel nodeModel, String id) {
		if (!nodeModel.isOpenStatus()) {
			return;
		}
		ThreadUtil.execute(() -> {
			try {
				JSONObject data = this.getItem(nodeModel, id);
				if (data == null) {
					return;
				}
				ProjectInfoModel projectInfoModel = data.toJavaObject(ProjectInfoModel.class);
				this.fullData(projectInfoModel, nodeModel);
				// 设置 临时缓存，便于放行检查
				BaseServerController.resetInfo(UserModel.EMPTY);
				//
				super.upsert(projectInfoModel);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("同步节点项目失败:" + nodeModel.getId(), e);
			} finally {
				BaseServerController.remove();
			}
		});
	}

	private void fullData(ProjectInfoModel projectInfoModel, NodeModel nodeModel) {
		projectInfoModel.setProjectId(projectInfoModel.getId());
		projectInfoModel.setId(null);
		projectInfoModel.setNodeId(nodeModel.getId());
		if (StrUtil.isEmpty(projectInfoModel.getWorkspaceId())) {
			projectInfoModel.setWorkspaceId(nodeModel.getWorkspaceId());
		}
		projectInfoModel.setId(projectInfoModel.fullId());
	}
}
