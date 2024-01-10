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
package org.dromara.jpom.service.node;

import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseNodeService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@Service
public class ProjectInfoCacheService extends BaseNodeService<ProjectInfoCacheModel> implements ITriggerToken {

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
        JsonMessage<JSONObject> request = NodeForward.request(nodeModel, NodeUrl.Manage_GetProjectItem, "id", id);
        return request.getData();
    }

    /**
     * 查询项目是否存在
     *
     * @param workspaceId 工作空间ID
     * @param nodeId      节点id
     * @param id          项目id
     * @return true 存在
     */
    public boolean exists(String workspaceId, String nodeId, String id) {
        ProjectInfoCacheModel projectInfoCacheModel = new ProjectInfoCacheModel();
        projectInfoCacheModel.setWorkspaceId(workspaceId);
        projectInfoCacheModel.setNodeId(nodeId);
        projectInfoCacheModel.setProjectId(id);
        return super.exists(projectInfoCacheModel);
    }

    /**
     * 查询项目是否存在
     *
     * @param nodeId 节点id
     * @param id     项目id
     * @return true 存在
     */
    public boolean exists(String nodeId, String id) {
        NodeModel nodeModel = nodeService.getByKey(nodeId);
        if (nodeModel == null) {
            return false;
        }
        return this.exists(nodeModel.getWorkspaceId(), nodeId, id);
    }

    /**
     * 将响应的数据转为请求的数据
     *
     * @param item 数据
     * @return data
     */
    public JSONObject convertToRequestData(JSONObject item) {

        return item;
    }


    @Override
    public JSONArray getLitDataArray(NodeModel nodeModel) {
        JsonMessage<JSONArray> tJsonMessage = NodeForward.request(nodeModel, NodeUrl.Manage_GetProjectInfo, "notStatus", "true");
        return tJsonMessage.getData();
    }

    @Override
    public List<ProjectInfoCacheModel> lonelyDataArray(MachineNodeModel machineNodeModel) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("notStatus", true);
        JsonMessage<JSONArray> tJsonMessage = NodeForward.request(machineNodeModel, NodeUrl.Manage_GetProjectInfo, jsonObject);
        return this.checkLonelyDataArray(tJsonMessage.getData(), machineNodeModel.getId());
    }

    @Override
    public String typeName() {
        return getTableName();
    }

    @Override
    protected void refreshCacheStat(String nodeId, int dataCount) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setId(nodeId);
        nodeModel.setJpomProjectCount(dataCount);
        nodeService.updateById(nodeModel);
    }
}
