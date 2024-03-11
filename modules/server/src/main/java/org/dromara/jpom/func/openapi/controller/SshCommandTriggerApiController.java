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
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerOpenApi;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.model.data.CommandModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.node.ssh.SshCommandService;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ssh 脚本触发器
 *
 * @author bwcx_jzy
 * @since 2022/7/25
 */
@RestController
@NotLogin
@Slf4j
public class SshCommandTriggerApiController extends BaseJpomController {

    private final SshCommandService sshCommandService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public SshCommandTriggerApiController(SshCommandService sshCommandService,
                                          TriggerTokenLogServer triggerTokenLogServer) {
        this.sshCommandService = sshCommandService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 执行脚本
     *
     * @param id    构建ID
     * @param token 构建的token
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.SSH_COMMAND_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> trigger2(@PathVariable String id, @PathVariable String token, HttpServletRequest request) {
        CommandModel item = sshCommandService.getByKey(id);
        Assert.notNull(item, "没有对应数据");
        Assert.state(StrUtil.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");
        //
        Assert.hasText(item.getSshIds(), "当前脚本未绑定 SSH 节点，不能使用触发器执行");
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshCommandService.typeName());
        //
        Assert.notNull(userModel, "触发token错误,或者已经失效:-1");
        // 解析参数
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        Map<String, String> newParamMap = new HashMap<>(10);
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            String key = StrUtil.format("trigger_{}", entry.getKey());
            key = StrUtil.toUnderlineCase(key);
            newParamMap.put(key, entry.getValue());
        }
        String batchId;
        try {
            BaseServerController.resetInfo(userModel);
            batchId = sshCommandService.executeBatch(item, item.getDefParams(), item.getSshIds(), 2, newParamMap);
        } catch (Exception e) {
            log.error("触发自动执行SSH命令模版异常", e);
            return new JsonMessage<>(500, "执行异常：" + e.getMessage());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("batchId", batchId);
        return JsonMessage.success("开始执行", jsonObject);
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
     * "batchId":"1",
     * "msg":"没有对应数据",
     * }
     * ]</code>
     *
     * @return json
     */
    @PostMapping(value = ServerOpenApi.SSH_COMMAND_TRIGGER_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<Object>> triggerBatch(HttpServletRequest request) {
        try {
            String body = ServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<Object> collect = jsonArray.stream().peek(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                CommandModel item = sshCommandService.getByKey(id);
                if (item == null) {
                    jsonObject.put("msg", "没有对应数据");
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshCommandService.typeName());
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
                String batchId = null;
                try {
                    batchId = sshCommandService.executeBatch(item, item.getDefParams(), item.getSshIds(), 2);
                } catch (Exception e) {
                    log.error("触发自动执行命令模版异常", e);
                    jsonObject.put("msg", "执行异常：" + e.getMessage());
                }
                jsonObject.put("batchId", batchId);
                //
            }).collect(Collectors.toList());
            return JsonMessage.success("触发成功", collect);
        } catch (Exception e) {
            log.error("SSH 脚本批量触发异常", e);
            return new JsonMessage<>(500, "触发异常" + e.getMessage());
        }
    }
}
