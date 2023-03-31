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
package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.BaseGroupNameController;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.dromara.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public MachineNodeController(WorkspaceService workspaceService,
                                 MachineNodeServer machineNodeServer) {
        super(machineNodeServer);
        this.workspaceService = workspaceService;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<MachineNodeModel>> listJson(HttpServletRequest request) {
        PageResultDto<MachineNodeModel> pageResultDto = machineNodeServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> save(HttpServletRequest request) {
        machineNodeServer.update(request);
        return JsonMessage.success("操作成功");
    }

    @PostMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> delete(@ValidatorItem String id) {
        long count = nodeService.countByMachine(id);
        Assert.state(count <= 0, "当前机器还关联" + count + "个节点不能删除");
        machineNodeServer.delByKey(id);
        return JsonMessage.success("操作成功");
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
    public JsonMessage<String> distribute(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        List<String> list = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String id : list) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
            Assert.notNull(machineNodeModel, "没有对应的机器");
            WorkspaceModel workspaceModel = new WorkspaceModel(workspaceId);
            boolean exists = workspaceService.exists(workspaceModel);
            Assert.state(exists, "不存在对应的工作空间");
            //
            if (!nodeService.existsNode2(workspaceId, id)) {
                //
                machineNodeServer.insertNode(machineNodeModel, workspaceId);
            }
        }

        return JsonMessage.success("操作成功");
    }

    @GetMapping(value = "list-node", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<NodeModel>> listData(@ValidatorItem String id) {
        MachineNodeModel machineNodeModel = machineNodeServer.getByKey(id);
        Assert.notNull(machineNodeModel, "没有对应的机器");
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
    public JsonMessage<List<MachineNodeModel>> listTemplate() {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setTemplateNode(true);
        List<MachineNodeModel> modelList = machineNodeServer.listByBean(machineNodeModel);
        return JsonMessage.success("", modelList);
    }


    /**
     * 保存白名单配置
     *
     * @return json
     */
    @RequestMapping(value = "save-whitelist", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_NODE_WHITELIST, method = MethodFeature.EDIT)
    public JsonMessage<Object> saveWhitelist(@ValidatorItem(msg = "请选择分发的机器") String ids,
                                             HttpServletRequest request) {
        //
        List<String> idList = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String s : idList) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(s);
            Assert.notNull(machineNodeModel, "没有对应的机器");
            JsonMessage<String> jsonMessage = NodeForward.request(machineNodeModel, request, NodeUrl.WhitelistDirectory_Submit);
            Assert.state(jsonMessage.success(), "分发 " + machineNodeModel.getName() + " 节点白名单失败" + jsonMessage.getMsg());
        }
        return JsonMessage.success("保存成功");
    }


    @PostMapping(value = "save-node-config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SYSTEM_CONFIG, method = MethodFeature.EDIT)
    @SystemPermission(superUser = true)
    public JsonMessage<Object> saveNodeConfig(@ValidatorItem(msg = "请选择分发的机器") String ids,
                                              String content,
                                              String restart) {
        List<String> idList = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String s : idList) {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(s);
            Assert.notNull(machineNodeModel, "没有对应的机器");
            JSONObject reqData = new JSONObject();
            reqData.put("content", content);
            reqData.put("restart", restart);
            JsonMessage<String> jsonMessage = NodeForward.request(machineNodeModel, NodeUrl.SystemSaveConfig, reqData);
            Assert.state(jsonMessage.success(), "分发 " + machineNodeModel.getName() + " 节点配置失败" + jsonMessage.getMsg());
        }
        return JsonMessage.success("修改成功");
    }
}
