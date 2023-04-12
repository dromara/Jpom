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
package org.dromara.jpom.controller.node.manage;

import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.NodeDataPermission;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 项目管理
 *
 * @author bwcx_jzy
 * @since 2018/9/29
 */
@RestController
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class EditProjectController extends BaseServerController {

    private final ProjectInfoCacheService projectInfoCacheService;
    private final WhitelistDirectoryService whitelistDirectoryService;

    public EditProjectController(ProjectInfoCacheService projectInfoCacheService,
                                 WhitelistDirectoryService whitelistDirectoryService) {
        this.projectInfoCacheService = projectInfoCacheService;
        this.whitelistDirectoryService = whitelistDirectoryService;
    }

    @RequestMapping(value = "getProjectData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> getProjectData(@ValidatorItem String id) {
        JSONObject projectInfo = projectInfoCacheService.getItem(getNode(), id);
        return JsonMessage.success("", projectInfo);
    }

    /**
     * get project access list
     * 获取项目的白名单
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "project-access-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<String>> projectAccessList() {
        List<String> jsonArray = whitelistDirectoryService.getProjectDirectory(getNode());
        return JsonMessage.success("success", jsonArray);
    }

    /**
     * 保存项目
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> saveProject(String id, HttpServletRequest httpServletRequest) {
        NodeModel node = getNode();

        //
        JsonMessage<String> request = NodeForward.request(node, httpServletRequest, NodeUrl.Manage_SaveProject, "outGivingProject");
        if (request.success()) {
            projectInfoCacheService.syncNode(node, id);
        }
        return request;
    }


    /**
     * 释放分发
     *
     * @return json
     */
    @RequestMapping(value = "release-outgiving", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> releaseOutgiving(String id, HttpServletRequest request) {
        NodeModel node = getNode();

        JsonMessage<String> jsonMessage = NodeForward.request(getNode(), request, NodeUrl.Manage_ReleaseOutGiving);
        if (jsonMessage.success()) {
            projectInfoCacheService.syncNode(node, id);
        }
        return jsonMessage;
    }
}
