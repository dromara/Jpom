/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node.manage.log;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.NodeDataPermission;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 控制台日志备份管理
 *
 * @author bwcx_jzy
 * @since 2019/3/7
 */
@Controller
@RequestMapping(value = "node/manage/log")
@Feature(cls = ClassFeature.PROJECT_LOG)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class LogBackController extends BaseServerController {

    private final ProjectInfoCacheService projectInfoCacheService;

    public LogBackController(ProjectInfoCacheService projectInfoCacheService) {
        this.projectInfoCacheService = projectInfoCacheService;
    }

    @RequestMapping(value = "export", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void export(HttpServletRequest request, HttpServletResponse response) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.Manage_Log_export);
    }

    /**
     * get log back list
     * 日志备份列表接口
     *
     * @return json
     * @author Hotstrip
     */
    @RequestMapping(value = "log-back-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<JSONObject> logBackList(HttpServletRequest request) {
        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Manage_Log_logBack, request, JSONObject.class);
        return JsonMessage.success("", jsonObject);
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(HttpServletResponse response, HttpServletRequest request) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.Manage_Log_logBack_download);
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> clear(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_Log_logBack_delete);
    }

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public IJsonMessage<JSONObject> logSize(String id) {

        return NodeForward.request(getNode(), NodeUrl.Manage_Log_LogSize, "id", id);
    }

    /**
     * 重置日志
     *
     * @return json
     */
    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> resetLog(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_Log_ResetLog);
    }
}
