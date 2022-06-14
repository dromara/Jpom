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
package io.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ProjectInfoCacheModel;
import io.jpom.model.node.ScriptCacheModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.node.script.NodeScriptExecuteLogServer;
import io.jpom.service.node.script.NodeScriptServer;
import io.jpom.service.outgiving.LogReadServer;
import io.jpom.service.outgiving.OutGivingServer;
import io.jpom.service.stat.NodeStatService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeEditController extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final MonitorService monitorService;
    private final BuildInfoService buildService;
    private final LogReadServer logReadServer;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeScriptServer nodeScriptServer;
    private final NodeStatService nodeStatService;
    private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;

    public NodeEditController(OutGivingServer outGivingServer,
                              MonitorService monitorService,
                              BuildInfoService buildService,
                              LogReadServer logReadServer,
                              ProjectInfoCacheService projectInfoCacheService,
                              NodeScriptServer nodeScriptServer,
                              NodeStatService nodeStatService,
                              NodeScriptExecuteLogServer nodeScriptExecuteLogServer) {
        this.outGivingServer = outGivingServer;
        this.monitorService = monitorService;
        this.buildService = buildService;
        this.logReadServer = logReadServer;
        this.projectInfoCacheService = projectInfoCacheService;
        this.nodeScriptServer = nodeScriptServer;
        this.nodeStatService = nodeStatService;
        this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
    }


    @PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String listJson() {
        PageResultDto<NodeModel> nodeModelPageResultDto = nodeService.listPage(getRequest());
        return JsonMessage.getString(200, "", nodeModelPageResultDto);
    }

    @GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String listDataAll() {
        List<NodeModel> list = nodeService.listByWorkspace(getRequest());
        return JsonMessage.getString(200, "", list);
    }

    @GetMapping(value = "list_data_by_workspace_id.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String listDataAll(@ValidatorItem String workspaceId) {
        nodeService.checkUserWorkspace(workspaceId);
        NodeModel nodeModel = new NodeModel();
        nodeModel.setWorkspaceId(workspaceId);
        List<NodeModel> list = nodeService.listByBean(nodeModel);
        return JsonMessage.getString(200, "", list);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list_group_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String listGroupAll() {
        List<String> listGroup = nodeService.listGroup(getRequest());
        return JsonMessage.getString(200, "", listGroup);
    }

    @RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String nodeStatus() {
        long timeMillis = System.currentTimeMillis();
        NodeModel node = getNode();
        JSONObject jsonObject = NodeForward.requestData(node, NodeUrl.Status, getRequest(), JSONObject.class);
        Assert.notNull(jsonObject, "获取信息失败");
        JSONArray jsonArray = new JSONArray();
        jsonObject.put("timeOut", System.currentTimeMillis() - timeMillis);
        jsonObject.put("nodeId", node.getId());
        jsonArray.add(jsonObject);
        return JsonMessage.getString(200, "", jsonArray);
    }

    @PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String save() {
        nodeService.update(getRequest(), false);
        return JsonMessage.getString(200, "操作成功");
    }


    /**
     * 删除节点
     *
     * @param id 节点id
     * @return json
     */
    @PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String del(@ValidatorItem String id) {
        HttpServletRequest request = getRequest();
        this.checkDataBind(id, request);
        //
        {
            ProjectInfoCacheModel projectInfoCacheModel = new ProjectInfoCacheModel();
            projectInfoCacheModel.setNodeId(id);
            projectInfoCacheModel.setWorkspaceId(projectInfoCacheService.getCheckUserWorkspace(request));
            boolean exists = projectInfoCacheService.exists(projectInfoCacheModel);
            Assert.state(!exists, "该节点下还存在项目，不能删除");
        }
        //
        {
            ScriptCacheModel scriptCacheModel = new ScriptCacheModel();
            scriptCacheModel.setNodeId(id);
            scriptCacheModel.setWorkspaceId(nodeScriptServer.getCheckUserWorkspace(request));
            boolean exists = nodeScriptServer.exists(scriptCacheModel);
            Assert.state(!exists, "该节点下还存在脚本模版，不能删除");
        }
        //
        this.delNodeData(id, request);
        return JsonMessage.getString(200, "操作成功");
    }

    private void checkDataBind(String id, HttpServletRequest request) {
        //  判断分发
        boolean checkNode = outGivingServer.checkNode(id, request);
        Assert.state(!checkNode, "该节点存在分发项目，不能删除");
        boolean checkLogRead = logReadServer.checkNode(id, request);
        Assert.state(!checkLogRead, "该日志阅读项目，不能删除");
        // 监控
        boolean checkNode1 = monitorService.checkNode(id);
        Assert.state(!checkNode1, "该节点存在监控项，不能删除");
        boolean checkNode2 = buildService.checkNode(id, request);
        Assert.state(!checkNode2, "该节点存在构建项，不能删除");
    }

    private void delNodeData(String id, HttpServletRequest request) {
        //
        int i = nodeService.delByKey(id, request);
        if (i > 0) {
            // 删除节点统计数据
            nodeStatService.delByKey(id);
            //
            nodeScriptExecuteLogServer.delCache(id, request);
        }
    }

    /**
     * 解绑
     *
     * @param id 分发id
     * @return json
     */
    @GetMapping(value = "unbind.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String unbind(String id) {
        HttpServletRequest request = getRequest();
        this.checkDataBind(id, request);
        //
        projectInfoCacheService.delCache(id, request);
        nodeScriptServer.delCache(id, request);
        this.delNodeData(id, request);
        return JsonMessage.getString(200, "操作成功");
    }

    /**
     * 解锁节点，通过插件端自动注册的节点默认未分配工作空间
     *
     * @param id          节点ID
     * @param workspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "un_lock_workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public String unLockWorkspace(@ValidatorItem String id, @ValidatorItem String workspaceId) {
        nodeService.checkUserWorkspace(workspaceId);
        nodeService.unLock(id, workspaceId);
        return JsonMessage.getString(200, "操作成功");
    }

    /**
     * 同步到指定工作空间
     *
     * @param ids         节点ID
     * @param workspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public String syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String workspaceId) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(getRequest());
        //
        nodeService.checkUserWorkspace(workspaceId);
        nodeService.syncToWorkspace(ids, nowWorkspaceId, workspaceId);
        return JsonMessage.getString(200, "操作成功");
    }
}
