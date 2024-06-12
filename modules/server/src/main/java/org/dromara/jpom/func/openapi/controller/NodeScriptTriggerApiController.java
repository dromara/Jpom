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
import org.dromara.jpom.model.node.NodeScriptCacheModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.script.NodeScriptServer;
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
 * 节点脚本触发器
 *
 * @author bwcx_jzy
 * @since 2022/7/25
 */
@RestController
@NotLogin
@Slf4j
public class NodeScriptTriggerApiController extends BaseJpomController {

    private final NodeScriptServer nodeScriptServer;
    private final NodeService nodeService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public NodeScriptTriggerApiController(NodeScriptServer nodeScriptServer,
                                          NodeService nodeService,
                                          TriggerTokenLogServer triggerTokenLogServer) {
        this.nodeScriptServer = nodeScriptServer;
        this.nodeService = nodeService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 执行脚本
     *
     * @param id    构建ID
     * @param token 构建的token
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.NODE_SCRIPT_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> trigger2(@PathVariable String id, @PathVariable String token, HttpServletRequest request) {
        NodeScriptCacheModel item = nodeScriptServer.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        Assert.state(StrUtil.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, nodeScriptServer.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");

        try {
            NodeModel nodeModel = nodeService.getByKey(item.getNodeId());
            JSONObject reqData = new JSONObject();
            reqData.put("id", item.getScriptId());
            reqData.put("params", JSONObject.toJSONString(ServletUtil.getParamMap(request)));
            JsonMessage<String> jsonMessage = NodeForward.request(nodeModel, NodeUrl.SCRIPT_EXEC, reqData);
            //
            JSONObject jsonObject = new JSONObject();
            if (jsonMessage.getCode() == 200) {
                jsonObject.put("logId", jsonMessage.getData());
            } else {
                jsonObject.put("msg", jsonMessage.getMsg());
            }
            return JsonMessage.success("开始执行", jsonObject);
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
     * "token":"a"
     * }
     * ]</code>
     * <p>
     * 响应 <code>[
     * {
     * "id":"1",
     * "token":"a",
     * "logId":"1",
     * "msg":"没有对应数据",
     * }
     * ]</code>
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.NODE_SCRIPT_TRIGGER_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<Object>> triggerBatch(HttpServletRequest request) {
        try {
            String body = ServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                NodeScriptCacheModel item = nodeScriptServer.getByKey(id);
                if (item == null) {
                    String string = "没有对应数据";
                    jsonObject.put("msg", string);
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, nodeScriptServer.typeName());
                if (userModel == null) {
                    String string = "对应的用户不存在,触发器已失效";
                    jsonObject.put("msg", string);
                    return;
                }
                //
                if (!StrUtil.equals(token, item.getTriggerToken())) {
                    String value = "触发token错误,或者已经失效";
                    jsonObject.put("msg", value);
                    return;
                }
                NodeModel nodeModel = nodeService.getByKey(item.getNodeId());
                JSONObject reqData = new JSONObject();
                reqData.put("id", item.getScriptId());
                JsonMessage<String> jsonMessage = NodeForward.request(nodeModel, NodeUrl.SCRIPT_EXEC, reqData);
                //
                if (jsonMessage.getCode() == 200) {
                    jsonObject.put("logId", jsonMessage.getData());
                } else {
                    jsonObject.put("msg", jsonMessage.getMsg());
                }
            }).collect(Collectors.toList());
            return JsonMessage.success("触发成功", collect);
        } catch (Exception e) {
            log.error("服务端脚本批量触发异常", e);
            return new JsonMessage<>(500, "触发异常" + e.getMessage());
        }
    }
}
