/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.outgiving;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.outgiving.LogReadModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.outgiving.LogReadServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志阅读
 *
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@RestController
@RequestMapping(value = "/log-read")
@Feature(cls = ClassFeature.LOG_READ)
public class LogReadController extends BaseServerController {

    private final LogReadServer logReadServer;

    public LogReadController(LogReadServer logReadServer) {
        this.logReadServer = logReadServer;
    }

    /**
     * 日志阅读列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<LogReadModel>> list(HttpServletRequest request) {
        PageResultDto<LogReadModel> pageResultDto = logReadServer.listPage(request);
        return JsonMessage.success("", pageResultDto);
    }

    /**
     * 删除日志阅读信息
     *
     * @param id 分发id
     * @return json
     */
    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> del(String id, HttpServletRequest request) {
        int byKey = logReadServer.delByKey(id, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 编辑日志阅读信息
     * <p>
     * {"projectList":[{"nodeId":"localhost","projectId":"test-jar"}],"name":"11"}
     *
     * @param jsonObject 参数
     * @return msg
     */
    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> save(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        Assert.notNull(jsonObject, I18nMessageUtil.get("i18n.please_pass_parameter.3182"));
        String id = jsonObject.getString("id");
        String name = jsonObject.getString("name");
        Assert.hasText(name, I18nMessageUtil.get("i18n.name_required.856d"));
        JSONArray projectListArray = jsonObject.getJSONArray("projectList");
        Assert.notEmpty(projectListArray, I18nMessageUtil.get("i18n.select_node_and_project.6021"));
        List<LogReadModel.Item> projectList = projectListArray.toJavaList(LogReadModel.Item.class);
        projectList = projectList.stream()
            .filter(item -> StrUtil.isAllNotEmpty(item.getNodeId(), item.getProjectId()))
            .collect(Collectors.toList());
        Assert.notEmpty(projectList, I18nMessageUtil.get("i18n.select_node_and_project.6021"));
        LogReadModel logReadModel = new LogReadModel();
        logReadModel.setId(id);
        logReadModel.setName(name);
        logReadModel.setNodeProject(JSONArray.toJSONString(projectList));
        //
        if (StrUtil.isEmpty(id)) {
            logReadServer.insert(logReadModel);
        } else {
            logReadServer.updateById(logReadModel, request);
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }

    /**
     * 更新缓存
     * <p>
     * {"op":"showlog","projectId":"python",
     * "search":true,"useProjectId":"python",
     * "useNodeId":"localhost",
     * "beforeCount":0,"afterCount":10,
     * "head":0,"tail":100,"first":"false",
     * "logFile":"/run.log"}
     *
     * @param jsonObject 参数
     * @return msg
     */
    @RequestMapping(value = "update-cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> updateCache(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        Assert.notNull(jsonObject, I18nMessageUtil.get("i18n.please_pass_parameter.3182"));
        String id = jsonObject.getString("id");
        Assert.hasText(id, I18nMessageUtil.get("i18n.please_pass_parameter.3182"));
        LogReadModel.CacheDta cacheDta = jsonObject.toJavaObject(LogReadModel.CacheDta.class);

        LogReadModel logReadModel = new LogReadModel();
        logReadModel.setId(id);
        logReadModel.setCacheData(JSONArray.toJSONString(cacheDta));
        logReadServer.updateById(logReadModel, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.modify_success.69be"));
    }
}
