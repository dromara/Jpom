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
package io.jpom.controller.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceEnvVarModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.system.WorkspaceEnvVarService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/12/10
 */

@RestController
@Feature(cls = ClassFeature.SYSTEM_WORKSPACE)
@RequestMapping(value = "/system/workspace_env/")
@SystemPermission
public class WorkspaceEnvVarController extends BaseServerController {

    private final WorkspaceEnvVarService workspaceEnvVarService;

    public WorkspaceEnvVarController(WorkspaceEnvVarService workspaceEnvVarService) {
        this.workspaceEnvVarService = workspaceEnvVarService;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public String list() {
        workspaceEnvVarService.getCheckUserWorkspace(getRequest());
        PageResultDto<WorkspaceEnvVarModel> listPage = workspaceEnvVarService.listPage(getRequest());
        listPage.each(workspaceEnvVarModel -> {
            Integer privacy = workspaceEnvVarModel.getPrivacy();
            if (privacy != null && privacy == 1) {
                workspaceEnvVarModel.setValue(StrUtil.EMPTY);
            }
        });
        return JsonMessage.getString(200, "", listPage);
    }

    /**
     * 编辑变量
     *
     * @param workspaceId 空间id
     * @param name        变量名称
     * @param value       值
     * @param description 描述
     * @return json
     */
    @PostMapping(value = "/edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public String edit(String id,
                       @ValidatorItem String workspaceId,
                       @ValidatorItem String name,
                       String value,
                       @ValidatorItem String description,
                       String privacy,
                       String nodeIds) {
        workspaceEnvVarService.checkUserWorkspace(workspaceId);

        this.checkInfo(id, name, workspaceId);
        boolean privacyBool = BooleanUtil.toBoolean(privacy);
        //
        WorkspaceEnvVarModel workspaceModel = new WorkspaceEnvVarModel();
        workspaceModel.setName(name);
        if (privacyBool) {
            if (StrUtil.isNotEmpty(value)) {
                workspaceModel.setValue(value);
            } else {
                // 隐私字段 创建必填
                Assert.state(StrUtil.isNotEmpty(id), "请填写参数值");
            }
        } else {
            // 非隐私必填
            Assert.hasText(value, "请填写参数值");
            workspaceModel.setValue(value);
        }
        workspaceModel.setWorkspaceId(workspaceId);
        workspaceModel.setNodeIds(nodeIds);
        workspaceModel.setDescription(description);
        //
        String oldNodeIds = null;
        if (StrUtil.isEmpty(id)) {
            // 创建
            workspaceModel.setPrivacy(privacyBool ? 1 : 0);
            workspaceEnvVarService.insert(workspaceModel);
        } else {
            WorkspaceEnvVarModel byKey = workspaceEnvVarService.getByKey(id);
            Assert.notNull(byKey, "没有对应的数据");
            Assert.state(StrUtil.equals(workspaceId, byKey.getWorkspaceId()), "选择工作空间错误");
            oldNodeIds = byKey.getNodeIds();
            workspaceModel.setId(id);
            // 不能修改
            workspaceModel.setPrivacy(null);
            workspaceEnvVarService.update(workspaceModel);
        }
        this.syncNodeEnvVar(workspaceModel, oldNodeIds);
        return JsonMessage.getString(200, "操作成功");
    }

    private void syncDelNodeEnvVar(String name, UserModel user, Collection<String> delNode, String workspaceId) {
        for (String s : delNode) {
            NodeModel byKey = nodeService.getByKey(s);
            Assert.state(StrUtil.equals(workspaceId, byKey.getWorkspaceId()), "选择节点错误");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", name);
            JsonMessage<String> jsonMessage = NodeForward.request(byKey, NodeUrl.Workspace_EnvVar_Delete, user, jsonObject);
            Assert.state(jsonMessage.getCode() == 200, "处理 " + byKey.getName() + " 节点删除脚本失败" + jsonMessage.getMsg());
        }
    }

    private void syncNodeEnvVar(WorkspaceEnvVarModel workspaceEnvVarModel, String oldNode) {
        String workspaceId = workspaceEnvVarModel.getWorkspaceId();
        List<String> newNodeIds = StrUtil.splitTrim(workspaceEnvVarModel.getNodeIds(), StrUtil.COMMA);
        List<String> oldNodeIds = StrUtil.splitTrim(oldNode, StrUtil.COMMA);
        Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
        UserModel user = getUser();
        // 删除
        this.syncDelNodeEnvVar(workspaceEnvVarModel.getName(), user, delNode, workspaceId);
        // 更新
        for (String newNodeId : newNodeIds) {
            NodeModel byKey = nodeService.getByKey(newNodeId);
            Assert.state(StrUtil.equals(workspaceId, byKey.getWorkspaceId()), "选择节点错误");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("description", workspaceEnvVarModel.getDescription());
            jsonObject.put("name", workspaceEnvVarModel.getName());
            if (StrUtil.isNotEmpty(workspaceEnvVarModel.getValue())) {
                jsonObject.put("value", workspaceEnvVarModel.getValue());
            } else {
                // 查询
                WorkspaceEnvVarModel byKeyExits = workspaceEnvVarService.getByKey(workspaceEnvVarModel.getId());
                jsonObject.put("value", byKeyExits.getValue());
            }
            JsonMessage<String> jsonMessage = NodeForward.request(byKey, NodeUrl.Workspace_EnvVar_Update, user, jsonObject);
            Assert.state(jsonMessage.getCode() == 200, "处理 " + byKey.getName() + " 节点同步脚本失败" + jsonMessage.getMsg());
        }
    }

    private void checkInfo(String id, String name, String workspaceId) {
        Validator.validateGeneral(name, 1, 50, "变量名称 1-50 英文字母 、数字和下划线");
        //
        Entity entity = Entity.create();
        entity.set("name", name);
        entity.set("workspaceId", workspaceId);
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        boolean exists = workspaceEnvVarService.exists(entity);
        Assert.state(!exists, "对应的变量名称已经存在啦");
    }


    /**
     * 删除变量
     *
     * @param id 变量 ID
     * @return json
     */
    @GetMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public String delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id,
                         @ValidatorItem String workspaceId) {
        workspaceEnvVarService.checkUserWorkspace(workspaceId);
        WorkspaceEnvVarModel byKey = workspaceEnvVarService.getByKey(id);
        Assert.notNull(byKey, "没有对应的数据");
        Assert.state(StrUtil.equals(workspaceId, byKey.getWorkspaceId()), "选择工作空间错误");
        String oldNodeIds = byKey.getNodeIds();
        List<String> delNode = StrUtil.splitTrim(oldNodeIds, StrUtil.COMMA);
        this.syncDelNodeEnvVar(byKey.getName(), getUser(), delNode, workspaceId);
        // 删除信息
        workspaceEnvVarService.delByKey(id);
        return JsonMessage.getString(200, "删除成功");
    }
}
