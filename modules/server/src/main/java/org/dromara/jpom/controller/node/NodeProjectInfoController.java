/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.UrlRedirectUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
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
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
@Slf4j
public class NodeProjectInfoController extends BaseServerController {

    private final ProjectInfoCacheService projectInfoCacheService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public NodeProjectInfoController(ProjectInfoCacheService projectInfoCacheService,
                                     TriggerTokenLogServer triggerTokenLogServer) {
        this.projectInfoCacheService = projectInfoCacheService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }


    /**
     * load node project list
     * 加载节点项目列表
     *
     * @return json
     * @author Hotstrip
     */
    @PostMapping(value = "project_list", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<PageResultDto<ProjectInfoCacheModel>> projectList(HttpServletRequest request) {
        PageResultDto<ProjectInfoCacheModel> resultDto = projectInfoCacheService.listPage(request);
        return JsonMessage.success("", resultDto);
    }

    /**
     * load node project list
     * 加载节点项目列表
     *
     * @return json
     * @author Hotstrip
     */
    @GetMapping(value = "project_list_all", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<ProjectInfoCacheModel>> projectListAll(HttpServletRequest request) {
        List<ProjectInfoCacheModel> projectInfoCacheModels = projectInfoCacheService.listByWorkspace(request);
        return JsonMessage.success("", projectInfoCacheModels);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list-project-group-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> listGroupAll(HttpServletRequest request) {
        List<String> listGroup = projectInfoCacheService.listGroup(request);
        return JsonMessage.success("", listGroup);
    }

    /**
     * 同步节点项目
     *
     * @return json
     */
    @GetMapping(value = "sync_project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
    public IJsonMessage<Object> syncProject(String nodeId, HttpServletRequest request) {
        NodeModel nodeModel = nodeService.getByKey(nodeId);
        Assert.notNull(nodeModel, I18nMessageUtil.get("i18n.node_not_exist.760e"));
        int count = projectInfoCacheService.delCache(nodeId, request);
        String msg = projectInfoCacheService.syncExecuteNode(nodeModel);
        return JsonMessage.success(I18nMessageUtil.get("i18n.active_clearance_colon.96a6") + count + StrUtil.SPACE + msg);
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
    public IJsonMessage<String> sortItem(@ValidatorItem String id, @ValidatorItem String method, String compareId, HttpServletRequest request) {
        if (StrUtil.equalsIgnoreCase(method, "top")) {
            projectInfoCacheService.sortToTop(id, request);
        } else if (StrUtil.equalsIgnoreCase(method, "up")) {
            projectInfoCacheService.sortMoveUp(id, compareId, request);
        } else if (StrUtil.equalsIgnoreCase(method, "down")) {
            projectInfoCacheService.sortMoveDown(id, compareId, request);
        } else {
            return new JsonMessage<>(400, I18nMessageUtil.get("i18n.unsupported_method.a1de") + method);
        }
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "project-trigger-url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Map<String, String>> getTriggerUrl(String id, String rest, HttpServletRequest request) {
        ProjectInfoCacheModel item = projectInfoCacheService.getByKey(id, request);
        UserModel user = getUser();
        ProjectInfoCacheModel updateItem;
        if (StrUtil.isEmpty(item.getTriggerToken()) || StrUtil.isNotEmpty(rest)) {
            updateItem = new ProjectInfoCacheModel();
            updateItem.setId(id);
            updateItem.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), projectInfoCacheService.typeName(),
                item.getId(), user.getId()));
            projectInfoCacheService.updateById(updateItem);
        } else {
            updateItem = item;
        }
        Map<String, String> map = this.getBuildToken(updateItem, request);
        String string = I18nMessageUtil.get("i18n.reset_success.faa3");
        return JsonMessage.success(StrUtil.isEmpty(rest) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(ProjectInfoCacheModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
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
