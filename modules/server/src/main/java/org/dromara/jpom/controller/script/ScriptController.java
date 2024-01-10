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
package org.dromara.jpom.controller.script;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.UrlRedirectUtil;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.script.CommandParam;
import org.dromara.jpom.service.node.script.NodeScriptServer;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@RestController
@RequestMapping(value = "/script")
@Feature(cls = ClassFeature.SCRIPT)
public class ScriptController extends BaseServerController {

    private final ScriptServer scriptServer;
    private final NodeScriptServer nodeScriptServer;
    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final TriggerTokenLogServer triggerTokenLogServer;
    private final WorkspaceService workspaceService;

    public ScriptController(ScriptServer scriptServer,
                            NodeScriptServer nodeScriptServer,
                            ScriptExecuteLogServer scriptExecuteLogServer,
                            TriggerTokenLogServer triggerTokenLogServer,
                            WorkspaceService workspaceService) {
        this.scriptServer = scriptServer;
        this.nodeScriptServer = nodeScriptServer;
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.workspaceService = workspaceService;
    }

    /**
     * get script list
     *
     * @return json
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<ScriptModel>> scriptList(HttpServletRequest request) {
        PageResultDto<ScriptModel> pageResultDto = scriptServer.listPage(request);
        return JsonMessage.success("success", pageResultDto);
    }

    /**
     * get script list
     *
     * @return json
     */
    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<ScriptModel>> scriptListAll(HttpServletRequest request) {
        List<ScriptModel> pageResultDto = scriptServer.listByWorkspace(request);
        return JsonMessage.success("success", pageResultDto);
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(String id,
                                     @ValidatorItem String context,
                                     @ValidatorItem String name,
                                     String autoExecCron,
                                     String defArgs,
                                     String description,
                                     String nodeIds,
                                     HttpServletRequest request) {
        ScriptModel scriptModel = new ScriptModel();
        scriptModel.setId(id);
        scriptModel.setContext(context);
        scriptModel.setName(name);
        scriptModel.setNodeIds(nodeIds);
        scriptModel.setDescription(description);
        scriptModel.setDefArgs(CommandParam.checkStr(defArgs));
        scriptModel.setWorkspaceId(scriptServer.covertGlobalWorkspace(request));

        Assert.hasText(scriptModel.getContext(), "内容为空");
        //
        scriptModel.setAutoExecCron(this.checkCron(autoExecCron));
        //
        String oldNodeIds = null;
        if (StrUtil.isEmpty(id)) {
            scriptServer.insert(scriptModel);
        } else {
            ScriptModel byKey = scriptServer.getByKeyAndGlobal(id, request);
            Assert.notNull(byKey, "没有对应的数据");
            oldNodeIds = byKey.getNodeIds();
            scriptServer.updateById(scriptModel, request);
        }
        this.syncNodeScript(scriptModel, oldNodeIds, request);
        return JsonMessage.success("修改成功");
    }

    private void syncDelNodeScript(ScriptModel scriptModel, Collection<String> delNode) {
        for (String nodeId : delNode) {
            NodeModel byKey = nodeService.getByKey(nodeId);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getId());
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.Script_Del, jsonObject);
            Assert.state(request.getCode() == 200, "处理 " + byKey.getName() + " 节点删除脚本失败" + request.getMsg());
            nodeScriptServer.syncNode(byKey);
        }
    }

    private void syncNodeScript(ScriptModel scriptModel, String oldNode, HttpServletRequest request) {
        List<String> oldNodeIds = StrUtil.splitTrim(oldNode, StrUtil.COMMA);
        List<String> newNodeIds = StrUtil.splitTrim(scriptModel.getNodeIds(), StrUtil.COMMA);
        Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
        // 删除
        this.syncDelNodeScript(scriptModel, delNode);
        // 更新
        for (String newNodeId : newNodeIds) {
            NodeModel byKey = nodeService.getByKey(newNodeId);
            Assert.notNull(byKey, "没有找到对应的节点");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getId());
            jsonObject.put("type", "sync");
            jsonObject.put("context", scriptModel.getContext());
            jsonObject.put("autoExecCron", scriptModel.getAutoExecCron());
            jsonObject.put("defArgs", scriptModel.getDefArgs());
            jsonObject.put("description", scriptModel.getDescription());
            jsonObject.put("name", scriptModel.getName());
            jsonObject.put("workspaceId", byKey.getWorkspaceId());
            jsonObject.put("global", scriptModel.global());
            jsonObject.put("nodeId", byKey.getId());
            JsonMessage<String> jsonMessage = NodeForward.request(byKey, NodeUrl.Script_Save, jsonObject);
            Assert.state(jsonMessage.success(), "处理 " + byKey.getName() + " 节点同步脚本失败" + jsonMessage.getMsg());
            nodeScriptServer.syncNode(byKey);
        }
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> del(String id, HttpServletRequest request) {
        ScriptModel server = scriptServer.getByKeyAndGlobal(id, request);
        if (server != null) {
            File file = server.scriptPath();
            boolean del = FileUtil.del(file);
            Assert.state(del, "清理脚本文件失败");
            // 删除节点中的脚本
            String nodeIds = server.getNodeIds();
            List<String> delNode = StrUtil.splitTrim(nodeIds, StrUtil.COMMA);
            this.syncDelNodeScript(server, delNode);
            scriptServer.delByKey(id, request);
            //
            scriptExecuteLogServer.delByWorkspace(request, entity -> entity.set("scriptId", id));
        }
        return JsonMessage.success("删除成功");
    }

    @GetMapping(value = "get", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> get(String id, HttpServletRequest request) {
        String workspaceId = scriptServer.getCheckUserWorkspace(request);
        ScriptModel server = scriptServer.getByKeyAndGlobal(id, request);
        Assert.notNull(server, "没有对应的脚本");
        String nodeIds = server.getNodeIds();
        List<String> newNodeIds = StrUtil.splitTrim(nodeIds, StrUtil.COMMA);
        List<JSONObject> nodeList = newNodeIds.stream()
            .map(s -> {
                JSONObject jsonObject = new JSONObject();
                NodeModel nodeModel = nodeService.getByKey(s);
                if (nodeModel == null) {
                    jsonObject.put("nodeName", "未知(数据丢失)");
                } else {
                    jsonObject.put("nodeName", nodeModel.getName());
                    jsonObject.put("nodeId", nodeModel.getId());
                    jsonObject.put("workspaceId", nodeModel.getWorkspaceId());
                    WorkspaceModel workspaceModel = workspaceService.getByKey(nodeModel.getWorkspaceId());
                    jsonObject.put("workspaceName", Optional.ofNullable(workspaceModel).map(WorkspaceModel::getName).orElse("未知(数据丢失)"));
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
        // 判断是否可以编辑节点
        boolean prohibitSync = nodeList.stream()
            .anyMatch(jsonObject -> {
                String workspaceId11 = (String) jsonObject.get("workspaceId");
                return !StrUtil.equals(workspaceId11, workspaceId);
            });
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", server);
        jsonObject.put("nodeList", nodeList);
        jsonObject.put("prohibitSync", prohibitSync);
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 释放脚本关联的节点
     *
     * @param id 脚本ID
     * @return json
     */
    @RequestMapping(value = "unbind.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public IJsonMessage<String> unbind(@ValidatorItem String id, HttpServletRequest request) {
        ScriptModel update = new ScriptModel();
        update.setId(id);
        update.setNodeIds(StrUtil.EMPTY);
        scriptServer.updateById(update, request);
        return JsonMessage.success("解绑成功");
    }

    /**
     * 同步到指定工作空间
     *
     * @param ids           节点ID
     * @param toWorkspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public IJsonMessage<String> syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String toWorkspaceId, HttpServletRequest request) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(request);
        //
        scriptServer.checkUserWorkspace(toWorkspaceId);
        scriptServer.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return JsonMessage.success("操作成功");
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Map<String, String>> getTriggerUrl(String id, String rest, HttpServletRequest request) {
        ScriptModel item = scriptServer.getByKey(id, request);
        UserModel user = getUser();
        ScriptModel updateInfo;
        if (StrUtil.isEmpty(item.getTriggerToken()) || StrUtil.isNotEmpty(rest)) {
            updateInfo = new ScriptModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), scriptServer.typeName(),
                item.getId(), user.getId()));
            scriptServer.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        return JsonMessage.success(StrUtil.isEmpty(rest) ? "ok" : "重置成功", map);
    }

    private Map<String, String> getBuildToken(ScriptModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.SERVER_SCRIPT_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.SERVER_SCRIPT_TRIGGER_BATCH);
        map.put("batchTriggerUrl", FileUtil.normalize(batchTriggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
