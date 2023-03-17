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
package io.jpom.func.openapi.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.BaseJpomController;
import io.jpom.common.JsonMessage;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ScriptCacheModel;
import io.jpom.model.user.UserModel;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.script.NodeScriptServer;
import io.jpom.service.user.TriggerTokenLogServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public JsonMessage<JSONObject> trigger2(@PathVariable String id, @PathVariable String token) {
        ScriptCacheModel item = nodeScriptServer.getByKey(id);
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
            reqData.put("params", JSONObject.toJSONString(ServletUtil.getParamMap(getRequest())));
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
    public JsonMessage<List<Object>> triggerBatch() {
        try {
            String body = ServletUtil.getBody(getRequest());
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                ScriptCacheModel item = nodeScriptServer.getByKey(id);
                if (item == null) {
                    jsonObject.put("msg", "没有对应数据");
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, nodeScriptServer.typeName());
                if (userModel == null) {
                    jsonObject.put("msg", "对应的用户不存在,触发器已失效");
                    return;
                }
                //
                if (!StrUtil.equals(token, item.getTriggerToken())) {
                    jsonObject.put("msg", "触发token错误,或者已经失效");
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
