/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node.manage.file;

import cn.keepbx.jpom.IJsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.NodeDataPermission;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 项目文件版本快照（构建发布触发的完整版本，用于快速回滚）
 *
 * @author bwcx_jzy
 * @since 2026/11/18
 */
@RestController
@RequestMapping(value = "/node/manage/file/")
@Feature(cls = ClassFeature.PROJECT_FILE)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class ProjectFileVersionController extends BaseServerController {

    /**
     * 能力探测
     *
     * @param id      项目ID
     * @param request 请求
     * @return json
     */
    @Feature(method = MethodFeature.LIST)
    @RequestMapping(value = "version-capability", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> capability(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Version_Capability);
    }

    /**
     * 发布前准备（首次开启时生成基线版本）
     *
     * @param id      项目ID
     * @param request 请求
     * @return json
     */
    @Feature(method = MethodFeature.EDIT)
    @RequestMapping(value = "version-begin", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> versionBegin(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Version_Begin);
    }

    /**
     * 生成版本快照（手动/构建登记）
     *
     * @param id      项目ID
     * @param request 请求
     * @return json
     */
    @Feature(method = MethodFeature.EDIT)
    @RequestMapping(value = "version-commit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> versionCommit(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Version_Commit);
    }

    /**
     * 版本列表
     *
     * @param id      项目ID
     * @param request 请求
     * @return json
     */
    @Feature(method = MethodFeature.LIST)
    @RequestMapping(value = "version-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> versionList(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Version_List);
    }

    /**
     * 切换版本（快速回滚）
     *
     * @param id      项目ID
     * @param request 请求
     * @return json
     */
    @Feature(method = MethodFeature.EDIT)
    @RequestMapping(value = "version-switch", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> versionSwitch(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Version_Switch);
    }

    /**
     * 删除版本
     *
     * @param id      项目ID
     * @param request 请求
     * @return json
     */
    @Feature(method = MethodFeature.DEL)
    @RequestMapping(value = "version-delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> versionDelete(String id, HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Version_Delete);
    }
}
