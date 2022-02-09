/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.service.node;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ProjectInfoCacheModel;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@Service
public class ProjectInfoCacheService extends BaseNodeService<ProjectInfoCacheModel> {


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
		ProjectInfoCacheModel projectInfoCacheModel = new ProjectInfoCacheModel();
		projectInfoCacheModel.setWorkspaceId(workspaceId);
		projectInfoCacheModel.setNodeId(nodeId);
		projectInfoCacheModel.setProjectId(id);
		return super.exists(projectInfoCacheModel);
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
