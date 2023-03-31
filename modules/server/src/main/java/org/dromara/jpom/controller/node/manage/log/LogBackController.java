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
package org.dromara.jpom.controller.node.manage.log;

import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JsonMessage;
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
 * @author jiangzeyin
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

    @RequestMapping(value = "export.html", method = RequestMethod.GET)
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
    public JsonMessage<JSONObject> logBackList() {
        JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Manage_Log_logBack, getRequest(), JSONObject.class);
        return JsonMessage.success("success", jsonObject);
    }

    @RequestMapping(value = "logBack_download", method = RequestMethod.GET)
    @ResponseBody
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(HttpServletResponse response) {
        NodeForward.requestDownload(getNode(), getRequest(), response, NodeUrl.Manage_Log_logBack_download);
    }

    @RequestMapping(value = "logBack_delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public String clear() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Log_logBack_delete).toString();
    }

    @RequestMapping(value = "logSize", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public JsonMessage<JSONObject> logSize(String id, String copyId) {
//        JSONObject info = projectInfoCacheService.getLogSize(getNode(), id, copyId);
        return NodeForward.request(getNode(), NodeUrl.Manage_Log_LogSize, "id", id, "copyId", copyId);
//        return requestData.getData();
//        return JsonMessage.success("", info);
    }

    /**
     * 重置日志
     *
     * @return json
     */
    @RequestMapping(value = "resetLog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.DEL)
    public String resetLog() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Log_ResetLog).toString();
    }
}
