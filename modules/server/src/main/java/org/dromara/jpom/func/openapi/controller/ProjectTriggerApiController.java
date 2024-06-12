/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目触发器
 *
 * @author bwcx_jzy
 * @since 2022/12/18
 */
@RestController
@RequestMapping
@NotLogin
@Slf4j
public class ProjectTriggerApiController extends BaseJpomController {

    private final ProjectInfoCacheService projectInfoCacheService;
    private final TriggerTokenLogServer triggerTokenLogServer;
    private final NodeService nodeService;

    public ProjectTriggerApiController(ProjectInfoCacheService projectInfoCacheService,
                                       TriggerTokenLogServer triggerTokenLogServer,
                                       NodeService nodeService) {
        this.projectInfoCacheService = projectInfoCacheService;
        this.triggerTokenLogServer = triggerTokenLogServer;
        this.nodeService = nodeService;
    }

    private NodeUrl resolveAction(String action) {
        if (StrUtil.equalsIgnoreCase(action, "stop")) {
            return NodeUrl.Manage_Operate;
        }
        if (StrUtil.equalsIgnoreCase(action, "start")) {
            return NodeUrl.Manage_Operate;
        }
        if (StrUtil.equalsIgnoreCase(action, "restart")) {
            return NodeUrl.Manage_Operate;
        }
        if (StrUtil.equalsIgnoreCase(action, "reload")) {
            return NodeUrl.Manage_Operate;
        }
        return NodeUrl.Manage_GetProjectStatus;
    }

    private JsonMessage<Object> execAction(ProjectInfoCacheModel item, String action) {
        NodeUrl resolveAction = this.resolveAction(action);
        //
        NodeModel nodeModel = nodeService.getByKey(item.getNodeId());
        return NodeForward.request(nodeModel, resolveAction,
            "id", item.getProjectId(), "opt", action);
    }

    /**
     * 执行脚本
     *
     * @param id    构建ID
     * @param token 构建的token
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.SERVER_PROJECT_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> trigger(@PathVariable String id, @PathVariable String token, String action) {
        ProjectInfoCacheModel item = projectInfoCacheService.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        Assert.state(StrUtil.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, projectInfoCacheService.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");

        try {
            return this.execAction(item, action);
        } catch (Exception e) {
            log.error("触发自动执行服务器脚本异常", e);
            return new JsonMessage<>(500, "执行异常：" + e.getMessage());
        }
    }


    /**
     * 构建触发器
     * <p>
     * 参数 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "action":"status"
     * }
     * ]</code>
     * <p>
     * 响应 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "code":"1",
     * "data":{},
     * "msg":"没有对应数据",
     * }
     * ]</code>
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.SERVER_PROJECT_TRIGGER_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> triggerBatch(HttpServletRequest request) {
        try {
            String body = ServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<JSONObject> collect = jsonArray.stream().map(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                String action = jsonObject.getString("action");
                ProjectInfoCacheModel item = projectInfoCacheService.getByKey(id);
                if (item == null) {
                    String value = "没有对应数据";
                    jsonObject.put("msg", value);
                    return jsonObject;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, projectInfoCacheService.typeName());
                if (userModel == null) {
                    String value = "对应的用户不存在,触发器已失效";
                    jsonObject.put("msg", value);
                    return jsonObject;
                }
                //
                if (!StrUtil.equals(token, item.getTriggerToken())) {
                    String value = "触发token错误,或者已经失效";
                    jsonObject.put("msg", value);
                    return jsonObject;
                }
                JsonMessage<Object> message = this.execAction(item, action);
                jsonObject.put("msg", message.getMsg());
                jsonObject.put("data", message.getData());
                jsonObject.put("code", message.getCode());
                return jsonObject;
            }).collect(Collectors.toList());
            return JsonMessage.success("触发成功", collect);
        } catch (Exception e) {
            log.error("项目批量触发异常", e);
            return new JsonMessage<>(500, "触发异常" + e.getMessage());
        }
    }
}
