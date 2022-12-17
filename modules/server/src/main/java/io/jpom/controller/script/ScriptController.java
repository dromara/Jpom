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
package io.jpom.controller.script;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.*;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.model.user.UserModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.script.NodeScriptServer;
import io.jpom.service.script.ScriptExecuteLogServer;
import io.jpom.service.script.ScriptServer;
import io.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public ScriptController(ScriptServer scriptServer,
                            NodeScriptServer nodeScriptServer,
                            ScriptExecuteLogServer scriptExecuteLogServer,
                            TriggerTokenLogServer triggerTokenLogServer) {
        this.scriptServer = scriptServer;
        this.nodeScriptServer = nodeScriptServer;
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * get script list
     *
     * @return json
     */
    @RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<ScriptModel>> scriptList() {
        PageResultDto<ScriptModel> pageResultDto = scriptServer.listPage(getRequest());
        return JsonMessage.success("success", pageResultDto);
    }

    /**
     * get script list
     *
     * @return json
     */
    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<ScriptModel>> scriptListAll() {
        List<ScriptModel> pageResultDto = scriptServer.listByWorkspace(getRequest());
        return JsonMessage.success("success", pageResultDto);
    }

    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> save(String id,
                                    @ValidatorItem String context,
                                    @ValidatorItem String name,
                                    String autoExecCron,
                                    String defArgs,
                                    String description, String nodeIds) {
        ScriptModel scriptModel = new ScriptModel();
        scriptModel.setId(id);
        scriptModel.setContext(context);
        scriptModel.setName(name);
        scriptModel.setNodeIds(nodeIds);
        scriptModel.setDescription(description);
        scriptModel.setDefArgs(defArgs);

        Assert.hasText(scriptModel.getContext(), "内容为空");
        //
        scriptModel.setAutoExecCron(this.checkCron(autoExecCron));
        //
        String oldNodeIds = null;
        if (StrUtil.isEmpty(id)) {
            scriptServer.insert(scriptModel);
        } else {
            HttpServletRequest request = getRequest();
            ScriptModel byKey = scriptServer.getByKey(id, request);
            Assert.notNull(byKey, "没有对应的数据");
            oldNodeIds = byKey.getNodeIds();
            scriptServer.updateById(scriptModel, request);
        }
        scriptModel.setWorkspaceId(scriptServer.getCheckUserWorkspace(getRequest()));
        this.syncNodeScript(scriptModel, oldNodeIds);
        return JsonMessage.success("修改成功");
    }

    private void syncDelNodeScript(ScriptModel scriptModel, UserModel user, Collection<String> delNode) {
        for (String s : delNode) {
            NodeModel byKey = nodeService.getByKey(s, getRequest());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getId());
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.Script_Del, user, jsonObject);
            Assert.state(request.getCode() == 200, "处理 " + byKey.getName() + " 节点删除脚本失败" + request.getMsg());
            nodeScriptServer.syncNode(byKey);
        }
    }

    private void syncNodeScript(ScriptModel scriptModel, String oldNode) {
        List<String> oldNodeIds = StrUtil.splitTrim(oldNode, StrUtil.COMMA);
        List<String> newNodeIds = StrUtil.splitTrim(scriptModel.getNodeIds(), StrUtil.COMMA);
        Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
        UserModel user = getUser();
        // 删除
        this.syncDelNodeScript(scriptModel, user, delNode);
        // 更新
        for (String newNodeId : newNodeIds) {
            NodeModel byKey = nodeService.getByKey(newNodeId, getRequest());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getId());
            jsonObject.put("type", "sync");
            jsonObject.put("context", scriptModel.getContext());
            jsonObject.put("autoExecCron", scriptModel.getAutoExecCron());
            jsonObject.put("defArgs", scriptModel.getDefArgs());
            jsonObject.put("description", scriptModel.getDescription());
            jsonObject.put("name", scriptModel.getName());
            jsonObject.put("workspaceId", scriptModel.getWorkspaceId());
            JsonMessage<String> request = NodeForward.request(byKey, NodeUrl.Script_Save, user, jsonObject);
            Assert.state(request.getCode() == 200, "处理 " + byKey.getName() + " 节点同步脚本失败" + request.getMsg());
            nodeScriptServer.syncNode(byKey);
        }
    }

    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> del(String id) {
        HttpServletRequest request = getRequest();
        ScriptModel server = scriptServer.getByKey(id, request);
        if (server != null) {
            File file = server.scriptPath();
            boolean del = FileUtil.del(file);
            Assert.state(del, "清理脚本文件失败");
            // 删除节点中的脚本
            String nodeIds = server.getNodeIds();
            List<String> delNode = StrUtil.splitTrim(nodeIds, StrUtil.COMMA);
            this.syncDelNodeScript(server, getUser(), delNode);
            scriptServer.delByKey(id, request);
            //
            scriptExecuteLogServer.delByWorkspace(request, entity -> entity.set("scriptId", id));
        }
        return JsonMessage.success("删除成功");
    }

    /**
     * 释放脚本关联的节点
     *
     * @param id 脚本ID
     * @return json
     */
    @RequestMapping(value = "unbind.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> unbind(@ValidatorItem String id) {
        ScriptModel update = new ScriptModel();
        update.setId(id);
        update.setNodeIds(StrUtil.EMPTY);
        scriptServer.updateById(update, getRequest());
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
    public JsonMessage<String> syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String toWorkspaceId) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(getRequest());
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
    public JsonMessage<Map<String, String>> getTriggerUrl(String id, String rest) {
        ScriptModel item = scriptServer.getByKey(id, getRequest());
        UserModel user = getUser();
        ScriptModel updateInfo;
        if (StrUtil.isEmpty(item.getTriggerToken()) || StrUtil.isNotEmpty(rest)) {
            updateInfo = new ScriptModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), scriptServer.typeName(),
                item.getId(), user.getId()));
            scriptServer.update(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo);
        return JsonMessage.success(StrUtil.isEmpty(rest) ? "ok" : "重置成功", map);
    }

    private Map<String, String> getBuildToken(ScriptModel item) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(getRequest(), ServerConst.PROXY_PATH);
        String url = ServerOpenApi.SERVER_SCRIPT_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerBuildUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.SERVER_SCRIPT_TRIGGER_BATCH);
        map.put("batchTriggerBuildUrl", FileUtil.normalize(batchTriggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
