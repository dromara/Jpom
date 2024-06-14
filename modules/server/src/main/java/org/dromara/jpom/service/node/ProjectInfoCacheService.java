/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.node;

import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
        super(nodeService, workspaceService, I18nMessageUtil.get("i18n.project_name.31ec"));
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
