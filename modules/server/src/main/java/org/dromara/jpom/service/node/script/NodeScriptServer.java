/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.node.script;

import cn.hutool.db.Entity;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.node.NodeScriptCacheModel;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseNodeService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2019/8/16
 */
@Service
public class NodeScriptServer extends BaseNodeService<NodeScriptCacheModel> implements ITriggerToken {


    public NodeScriptServer(NodeService nodeService,
                            WorkspaceService workspaceService) {
        super(nodeService, workspaceService, I18nMessageUtil.get("i18n.script_template.1f77"));
    }

    /**
     * 查询操作脚本 模版的节点
     *
     * @return nodeId list
     */
    public List<String> hasScriptNode() {
        String sql = "select nodeId from " + super.getTableName() + " group by nodeId ";
        List<Entity> query = super.query(sql);
        if (query == null) {
            return null;
        }
        return query.stream().map(entity -> entity.getStr("nodeId")).collect(Collectors.toList());
    }

    @Override
    public JSONObject getItem(NodeModel nodeModel, String id) {
        return null;
    }

    @Override
    public JSONArray getLitDataArray(NodeModel nodeModel) {
        return NodeForward.requestData(nodeModel, NodeUrl.Script_List, null, JSONArray.class);
    }

    @Override
    public List<NodeScriptCacheModel> lonelyDataArray(MachineNodeModel machineNodeModel) {
        JSONArray jsonArray = NodeForward.requestData(machineNodeModel, NodeUrl.Script_List, null, JSONArray.class);
        return this.checkLonelyDataArray(jsonArray, machineNodeModel.getId());
    }

    @Override
    public String typeName() {
        return getTableName();
    }

    @Override
    protected void refreshCacheStat(String nodeId, int dataCount) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setId(nodeId);
        nodeModel.setJpomScriptCount(dataCount);
        nodeService.updateById(nodeModel);
    }
}
