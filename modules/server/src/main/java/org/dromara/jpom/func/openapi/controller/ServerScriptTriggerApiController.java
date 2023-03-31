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
package org.dromara.jpom.func.openapi.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.model.script.ScriptExecuteLogModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.socket.ServerScriptProcessBuilder;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务端脚本触发器
 *
 * @author bwcx_jzy
 * @since 2022/7/25
 */
@RestController
@NotLogin
@Slf4j
public class ServerScriptTriggerApiController extends BaseJpomController {

    private final ScriptServer scriptServer;
    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public ServerScriptTriggerApiController(ScriptServer scriptServer,
                                            ScriptExecuteLogServer scriptExecuteLogServer,
                                            TriggerTokenLogServer triggerTokenLogServer) {
        this.scriptServer = scriptServer;
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 执行脚本
     *
     * @param id    构建ID
     * @param token 构建的token
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.SERVER_SCRIPT_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<JSONObject> trigger2(@PathVariable String id, @PathVariable String token) {
        ScriptModel item = scriptServer.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        Assert.state(StrUtil.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        //
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, scriptServer.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");

        try {
            BaseServerController.resetInfo(userModel);
            // 解析参数
            Map<String, String> paramMap = ServletUtil.getParamMap(getRequest());
            Map<String, String> newParamMap = new HashMap<>(10);
            for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                String key = StrUtil.format("trigger_{}", entry.getKey());
                key = StrUtil.toUnderlineCase(key);
                newParamMap.put(key, entry.getValue());
            }
            // 创建记录
            ScriptExecuteLogModel nodeScriptExecLogModel = scriptExecuteLogServer.create(item, 2);
            // 执行
            ServerScriptProcessBuilder.create(item, nodeScriptExecLogModel.getId(), item.getDefArgs(), newParamMap);
            //
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("logId", nodeScriptExecLogModel.getId());
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
    @PostMapping(value = ServerOpenApi.SERVER_SCRIPT_TRIGGER_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<Object>> triggerBatch() {
        try {
            String body = ServletUtil.getBody(getRequest());
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                ScriptModel item = scriptServer.getByKey(id);
                if (item == null) {
                    jsonObject.put("msg", "没有对应数据");
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, scriptServer.typeName());
                if (userModel == null) {
                    jsonObject.put("msg", "对应的用户不存在,触发器已失效");
                    return;
                }
                //
                if (!StrUtil.equals(token, item.getTriggerToken())) {
                    jsonObject.put("msg", "触发token错误,或者已经失效");
                    return;
                }
                BaseServerController.resetInfo(userModel);
                try {
                    // 创建记录
                    ScriptExecuteLogModel nodeScriptExecLogModel = scriptExecuteLogServer.create(item, 2);
                    // 执行
                    ServerScriptProcessBuilder.create(item, nodeScriptExecLogModel.getId(), item.getDefArgs());
                    jsonObject.put("logId", nodeScriptExecLogModel.getId());
                } catch (Exception e) {
                    log.error("触发自动执行命令模版异常", e);
                    jsonObject.put("msg", "执行异常：" + e.getMessage());
                }
                //
            }).collect(Collectors.toList());
            return JsonMessage.success("触发成功", collect);
        } catch (Exception e) {
            log.error("服务端脚本批量触发异常", e);
            return new JsonMessage<>(500, "触发异常" + e.getMessage());
        }
    }
}
