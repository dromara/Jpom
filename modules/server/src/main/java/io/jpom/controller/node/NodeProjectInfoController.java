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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import io.jpom.common.*;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ProjectInfoCacheModel;
import io.jpom.model.user.UserModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeProjectInfoController extends BaseServerController {

    private final ProjectInfoCacheService projectInfoCacheService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public NodeProjectInfoController(ProjectInfoCacheService projectInfoCacheService,
                                     TriggerTokenLogServer triggerTokenLogServer) {
        this.projectInfoCacheService = projectInfoCacheService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * @return json
     * @author Hotstrip
     * load node project list
     * 加载节点项目列表
     */
    @PostMapping(value = "node_project_list", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<PageResultDto<ProjectInfoCacheModel>> nodeProjectList() {
        PageResultDto<ProjectInfoCacheModel> resultDto = projectInfoCacheService.listPageNode(getRequest());
        return JsonMessage.success("success", resultDto);
    }


    /**
     * load node project list
     * 加载节点项目列表
     *
     * @return json
     * @author Hotstrip
     */
    @PostMapping(value = "project_list", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<PageResultDto<ProjectInfoCacheModel>> projectList() {
        PageResultDto<ProjectInfoCacheModel> resultDto = projectInfoCacheService.listPage(getRequest());
        return JsonMessage.success("success", resultDto);
    }

    /**
     * load node project list
     * 加载节点项目列表
     *
     * @return json
     * @author Hotstrip
     */
    @GetMapping(value = "project_list_all", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<ProjectInfoCacheModel>> projectListAll() {
        List<ProjectInfoCacheModel> projectInfoCacheModels = projectInfoCacheService.listByWorkspace(getRequest());
        return JsonMessage.success("", projectInfoCacheModels);
    }

    /**
     * 同步节点项目
     *
     * @return json
     */
    @GetMapping(value = "sync_project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
    public JsonMessage<Object> syncProject(String nodeId) {
        NodeModel nodeModel = nodeService.getByKey(nodeId);
        Assert.notNull(nodeModel, "对应的节点不存在");
        int count = projectInfoCacheService.delCache(nodeId, getRequest());
        String msg = projectInfoCacheService.syncExecuteNode(nodeModel);
        return JsonMessage.success("主动清除：" + count + StrUtil.SPACE + msg);
    }

    /**
     * 删除节点缓存的所有项目
     *
     * @return json
     */
    @GetMapping(value = "clear_all_project", produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission(superUser = true)
    @Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
    public JsonMessage<Object> clearAll() {
        Entity where = Entity.create();
        where.set("id", " <> id");
        int del = projectInfoCacheService.del(where);
        return JsonMessage.success("成功删除" + del + "条项目缓存");
    }

    /**
     * 排序
     *
     * @param id        节点ID
     * @param method    方法
     * @param compareId 比较的ID
     * @return msg
     */
    @GetMapping(value = "project-sort-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId) {
        HttpServletRequest request = getRequest();
        if (StrUtil.equalsIgnoreCase(method, "top")) {
            projectInfoCacheService.sortToTop(id, request);
        } else if (StrUtil.equalsIgnoreCase(method, "up")) {
            projectInfoCacheService.sortMoveUp(id, compareId, request);
        } else if (StrUtil.equalsIgnoreCase(method, "down")) {
            projectInfoCacheService.sortMoveDown(id, compareId, request);
        } else {
            return new JsonMessage<>(400, "不支持的方式" + method);
        }
        return new JsonMessage<>(200, "操作成功");
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "project-trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Map<String, String>> getTriggerUrl(String id, String rest) {
        ProjectInfoCacheModel item = projectInfoCacheService.getByKey(id, getRequest());
        UserModel user = getUser();
        ProjectInfoCacheModel updateItem;
        if (StrUtil.isEmpty(item.getTriggerToken()) || StrUtil.isNotEmpty(rest)) {
            updateItem = new ProjectInfoCacheModel();
            updateItem.setId(id);
            updateItem.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), projectInfoCacheService.typeName(),
                item.getId(), user.getId()));
            projectInfoCacheService.update(updateItem);
        } else {
            updateItem = item;
        }
        Map<String, String> map = this.getBuildToken(updateItem);
        return JsonMessage.success(StrUtil.isEmpty(rest) ? "ok" : "重置成功", map);
    }

    private Map<String, String> getBuildToken(ProjectInfoCacheModel item) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(getRequest(), ServerConst.PROXY_PATH);
        String url = ServerOpenApi.SERVER_PROJECT_TRIGGER_URL.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.SERVER_PROJECT_TRIGGER_BATCH);
        map.put("batchTriggerUrl", FileUtil.normalize(batchTriggerBuildUrl));

        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
