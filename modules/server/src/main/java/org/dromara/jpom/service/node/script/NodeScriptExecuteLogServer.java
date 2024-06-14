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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.exception.AgentException;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.BaseDbModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.node.NodeScriptExecuteLogCacheModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseNodeService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 脚本默认执行记录
 *
 * @author bwcx_jzy
 * @since 2021/12/12
 */
@Service
@Slf4j
public class NodeScriptExecuteLogServer extends BaseNodeService<NodeScriptExecuteLogCacheModel> {

    public NodeScriptExecuteLogServer(NodeService nodeService,
                                      WorkspaceService workspaceService) {
        super(nodeService, workspaceService, I18nMessageUtil.get("i18n.script_template_log2.6b2c"));
    }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"createTimeMillis"};
    }

    @Override
    public JSONObject getItem(NodeModel nodeModel, String id) {
        return null;
    }

    @Override
    public JSONArray getLitDataArray(NodeModel nodeModel) {
        JsonMessage<?> jsonMessage = NodeForward.request(nodeModel, NodeUrl.SCRIPT_PULL_EXEC_LOG, "pullCount", 100);
        if (!jsonMessage.success()) {
            throw new AgentException(jsonMessage.toString());
        }
        Object data = jsonMessage.getData();
        //
        JSONArray jsonArray = (JSONArray) JSON.toJSON(data);
        for (Object o : jsonArray) {
            JSONObject jsonObject = (JSONObject) o;
            jsonObject.put("nodeId", nodeModel.getId());
            // 自动
            if (!jsonObject.containsKey("triggerExecType")) {
                jsonObject.put("triggerExecType", 1);
            }
        }
        return jsonArray;
    }

    @Override
    public List<NodeScriptExecuteLogCacheModel> lonelyDataArray(MachineNodeModel machineNodeModel) {
        throw new IllegalStateException(I18nMessageUtil.get("i18n.unsupported_mode_with_script_log.6a7a"));
    }

    @Override
    public void syncAllNode() {
        //
    }

    /**
     * 同步执行 同步节点信息(增量)
     *
     * @param nodeModel 节点信息
     * @return json
     */
    public Collection<String> syncExecuteNodeInc(NodeModel nodeModel) {
        String nodeModelName = nodeModel.getName();
        if (!nodeModel.isOpenStatus()) {
            log.debug(I18nMessageUtil.get("i18n.node_not_enabled.10ef"), nodeModelName);
            return null;
        }
        try {
            JSONArray jsonArray = this.getLitDataArray(nodeModel);
            if (CollUtil.isEmpty(jsonArray)) {
                //
                return null;
            }
            //
            List<NodeScriptExecuteLogCacheModel> models = jsonArray.toJavaList(this.tClass)
                .stream()
                .filter(item -> {
                    if (StrUtil.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 检查对应的工作空间 是否存在
                    return workspaceService.exists(new WorkspaceModel(item.getWorkspaceId()));
                })
                .filter(item -> {
                    if (StrUtil.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 避免重复同步
                    return StrUtil.equals(nodeModel.getWorkspaceId(), item.getWorkspaceId());
                })
                .collect(Collectors.toList());
            // 设置 临时缓存，便于放行检查
            BaseServerController.resetInfo(UserModel.EMPTY);
            //
            models.forEach(NodeScriptExecuteLogServer.super::upsert);
            String template = I18nMessageUtil.get("i18n.physical_node_pull_records.99df");
            String format = StrUtil.format(
                template,
                nodeModelName, CollUtil.size(jsonArray),
                CollUtil.size(models));
            log.debug(format);
            return models.stream().map(BaseDbModel::getId).collect(Collectors.toList());
        } catch (Exception e) {
            this.checkException(e, nodeModelName);
            return null;
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("createTimeMillis", h2DbLogStorageCount,
            null,
            executeLogModel -> {
                try {
                    NodeModel nodeModel = nodeService.getByKey(executeLogModel.getNodeId());
                    JsonMessage<Object> jsonMessage = NodeForward.request(nodeModel, NodeUrl.SCRIPT_DEL_LOG,
                        "id", executeLogModel.getScriptId(), "executeId", executeLogModel.getId());
                    if (!jsonMessage.success()) {
                        log.warn("{} {} {}", executeLogModel.getNodeId(), executeLogModel.getScriptName(), jsonMessage);
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.auto_clear_data_errors.112f"), executeLogModel.getNodeId(), executeLogModel.getScriptName(), e);
                    return false;
                }
            });
    }
}
