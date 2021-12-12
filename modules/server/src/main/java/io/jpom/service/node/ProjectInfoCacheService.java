package io.jpom.service.node;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@Service
public class ProjectInfoCacheService extends BaseNodeService<ProjectInfoModel> {


	public ProjectInfoCacheService(NodeService nodeService,
								   WorkspaceService workspaceService) {
		super(nodeService, workspaceService, "项目");
	}

	/**
	 * 查询远端项目
	 *
	 * @param nodeModel 节点ID
	 * @param id        项目ID
	 * @return json
	 */
	@Override
	public JSONObject getItem(NodeModel nodeModel, String id) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectItem, JSONObject.class, "id", id);
	}

	public boolean exists(String workspaceId, String nodeId, String id) {
		ProjectInfoModel projectInfoModel = new ProjectInfoModel();
		projectInfoModel.setWorkspaceId(workspaceId);
		projectInfoModel.setNodeId(nodeId);
		projectInfoModel.setProjectId(id);
		return super.exists(projectInfoModel);
	}

	public boolean exists(String nodeId, String id) {
		NodeModel nodeModel = nodeService.getByKey(nodeId);
		if (nodeModel == null) {
			return false;
		}
		return this.exists(nodeModel.getWorkspaceId(), nodeId, id);
	}

	public JSONObject getLogSize(NodeModel nodeModel, String id, String copyId) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_Log_LogSize, JSONObject.class, "id", id, "copyId", copyId);
	}




	@Override
	public JSONArray getLitDataArray(NodeModel nodeModel) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectInfo, JSONArray.class, "notStatus", "true");
	}
}
