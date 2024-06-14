/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.BaseGroupNameController;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.node.NodeScriptCacheModel;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.node.script.NodeScriptServer;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 机器节点
 *
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@RestController
@RequestMapping(value = "/system/assets/machine")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
@SystemPermission
public class MachineNodeController extends BaseGroupNameController {

    private final WorkspaceService workspaceService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeScriptServer nodeScriptServer;
    private final NodeService nodeService;

    public MachineNodeController(WorkspaceService workspaceService,
                                 MachineNodeServer machineNodeServer,
                                 ProjectInfoCacheService projectInfoCacheService,
                                 NodeScriptServer nodeScriptServer,
                                 NodeService nodeService) {
        super(machineNodeServer);
        this.workspaceService = workspaceService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.nodeScriptServer = nodeScriptServer;
        this.nodeService = nodeService;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<MachineNodeModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineNodeModel> pageResultDto = machineNodeServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    @GetMapping(value = "search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<MachineNodeModel>> search(String name, String appendIds, int limit) {
        Entity entity = new Entity();
        if (StrUtil.isNotEmpty(name)) {
            entity.set("name", StrUtil.format(" like '%{}%'", name));
        }
        limit = Math.max(limit, 1);
        List<String> appendIdList = StrUtil.splitTrim(appendIds, StrUtil.COMMA);
        List<MachineNodeModel> machineNodeModels = machineNodeServer.queryList(entity, limit, machineNodeServer.defaultOrders());
        appendIdList = appendIdList.stream()
            .filter(s -> machineNodeModels.stream()
                .noneMatch(machineNodeModel -> StrUtil.equals(s, machineNodeModel.getId())))
            .collect(Collectors.toList());
        for (String s : appendIdList) {
            MachineNodeModel nodeModel = machineNodeServer.getByKey(s);
            if (nodeModel == null) {
                continue;
            }
            machineNodeModels.add(nodeModel);
        }
        return JsonMessage.success("", machineNodeModels);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(HttpServletRequest request) {
        machineNodeServer.update(request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    @PostMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> delete(@ValidatorItem String id) {
        long count = nodeService.countByMachine(id);
        Assert.state(count <= 0, StrUtil.format(I18nMessageUtil.get("i18n.associated_nodes_warning.64d8"), count));
        machineNodeServer.delByKey(id);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 将机器分配到指定工作空间
     *
     * @param ids         机器id
     * @param workspaceId 工作空间id
     * @return json
     */
    @PostMapping(value = "distribute", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> distribute(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        List<String> list = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String id : list) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
            Assert.notNull(machineNodeModel, I18nMessageUtil.get("i18n.no_machine.89ed"));
            WorkspaceModel workspaceModel = new WorkspaceModel(workspaceId);
            boolean exists = workspaceService.exists(workspaceModel);
            Assert.state(exists, I18nMessageUtil.get("i18n.workspace_not_exist.a6fd"));
            //
            if (!nodeService.existsNode2(workspaceId, id)) {
                //
                machineNodeServer.insertNode(machineNodeModel, workspaceId);
            }
        }

        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    @GetMapping(value = "list-node", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<NodeModel>> listData(@ValidatorItem String id) {
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
        Assert.notNull(machineNodeModel, I18nMessageUtil.get("i18n.no_machine.89ed"));
        NodeModel nodeModel = new NodeModel();
        nodeModel.setMachineId(id);
        List<NodeModel> modelList = nodeService.listByBean(nodeModel);
        modelList = Optional.ofNullable(modelList).orElseGet(ArrayList::new);
        for (NodeModel model : modelList) {
            model.setWorkspace(workspaceService.getByKey(model.getWorkspaceId()));
        }
        return JsonMessage.success("", modelList);
    }

    /**
     * 查询模板节点
     *
     * @return list
     */
    @GetMapping(value = "list-template-node", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<MachineNodeModel>> listTemplate() {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setTemplateNode(true);
        List<MachineNodeModel> modelList = machineNodeServer.listByBean(machineNodeModel);
        return JsonMessage.success("", modelList);
    }


    /**
     * 保存授权配置
     *
     * @return json
     */
    @RequestMapping(value = "save-whitelist", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_NODE_WHITELIST, method = MethodFeature.EDIT)
    public IJsonMessage<Object> saveWhitelist(@ValidatorItem(msg = "i18n.distribution_machine_required.5921") String ids,
                                              HttpServletRequest request) {
        //
        List<String> idList = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String s : idList) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(s);
            Assert.notNull(machineNodeModel, I18nMessageUtil.get("i18n.no_machine.89ed"));
            JsonMessage<String> jsonMessage = NodeForward.request(machineNodeModel, request, NodeUrl.WhitelistDirectory_Submit);
            Assert.state(jsonMessage.success(), StrUtil.format(I18nMessageUtil.get("i18n.distribute_node_authorization_failure.bb92"), machineNodeModel.getName(), jsonMessage.getMsg()));
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.save_succeeded.3b10"));
    }


    @PostMapping(value = "save-node-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG, method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public IJsonMessage<Object> saveNodeConfig(@ValidatorItem(msg = "i18n.distribution_machine_required.5921") String ids,
                                               String content,
                                               String restart) {
        List<String> idList = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String s : idList) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(s);
            Assert.notNull(machineNodeModel, I18nMessageUtil.get("i18n.no_machine.89ed"));
            JSONObject reqData = new JSONObject();
            reqData.put("content", content);
            reqData.put("restart", restart);
            JsonMessage<String> jsonMessage = NodeForward.request(machineNodeModel, NodeUrl.SystemSaveConfig, reqData);
            Assert.state(jsonMessage.success(), StrUtil.format(I18nMessageUtil.get("i18n.distribute_node_configuration_failure.8146"), machineNodeModel.getName(), jsonMessage.getMsg()));
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }

    /**
     * 查询集群孤独的数据
     *
     * @param id 集群ID
     * @return json
     */
    @GetMapping(value = "lonely-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Object> lonelyData(@ValidatorItem String id) {
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
        Assert.notNull(machineNodeModel, I18nMessageUtil.get("i18n.no_machine.89ed"));
        List<ProjectInfoCacheModel> models = projectInfoCacheService.lonelyDataArray(machineNodeModel);
        List<NodeScriptCacheModel> scriptCacheModels = nodeScriptServer.lonelyDataArray(machineNodeModel);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("projects", models);
        jsonObject.put("scripts", scriptCacheModels);
        return JsonMessage.success("", jsonObject);
    }

    @PostMapping(value = "correct-lonely-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<Object> correctLonelyData(@ValidatorItem String id,
                                                  @ValidatorItem String type,
                                                  @ValidatorItem String dataId,
                                                  @ValidatorItem String toNodeId) {
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
        Assert.notNull(machineNodeModel, I18nMessageUtil.get("i18n.no_machine.89ed"));
        {
            NodeModel nodeModel = nodeService.getByKey(toNodeId);
            Assert.notNull(nodeModel, I18nMessageUtil.get("i18n.no_node.2e83"));
            Assert.hasText(nodeModel.getWorkspaceId(), I18nMessageUtil.get("i18n.node_has_no_workspace.69c0"));
            Assert.state(StrUtil.equals(nodeModel.getMachineId(), machineNodeModel.getId()), I18nMessageUtil.get("i18n.asset_cluster_and_node_mismatch.8964"));
            NodeUrl nodeUrl;
            if (StrUtil.equalsIgnoreCase(type, "script")) {
                nodeUrl = NodeUrl.Script_ChangeWorkspaceId;
            } else if (StrUtil.equalsIgnoreCase(type, "project")) {
                nodeUrl = NodeUrl.Manage_ChangeWorkspaceId;
            } else {
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_type_with_colon2.7de2") + type);
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("newWorkspaceId", nodeModel.getWorkspaceId());
            jsonObject.put("newNodeId", toNodeId);
            jsonObject.put("id", dataId);
            JsonMessage<String> jsonMessage = NodeForward.request(machineNodeModel, nodeUrl, jsonObject);
            if (!jsonMessage.success()) {
                return new JsonMessage<>(406, I18nMessageUtil.get("i18n.correction_data_failure.dac6") + jsonMessage.getMsg());
            }
        }
        // 重新同步节点数据
        {
            NodeModel nodeModel = new NodeModel();
            nodeModel.setMachineId(id);
            List<NodeModel> modelList = nodeService.listByBean(nodeModel);
            for (NodeModel model : modelList) {
                if (StrUtil.equalsIgnoreCase(type, "script")) {
                    nodeScriptServer.syncExecuteNode(model);
                } else if (StrUtil.equalsIgnoreCase(type, "project")) {
                    projectInfoCacheService.syncExecuteNode(model);
                }
            }
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.correction_success.38bc"));
    }

    @GetMapping(value = "monitor-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> monitorConfig(HttpServletRequest request, String id) {
        IJsonMessage<JSONObject> message = this.tryRequestMachine(id, request, NodeUrl.Info);
        Assert.notNull(message, I18nMessageUtil.get("i18n.no_asset_machine.c77c"));
        Assert.state(message.success(), message.getMsg());
        JSONObject data = message.getData();
        JSONObject monitor = Optional.ofNullable(data).map(jsonObject -> jsonObject.getJSONObject("monitor")).orElse(null);
        return JsonMessage.success("", monitor);
    }
}
