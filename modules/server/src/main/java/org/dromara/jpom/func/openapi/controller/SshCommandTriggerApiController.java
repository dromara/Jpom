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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
        Assert.notNull(item, I18nMessageUtil.get("i18n.no_data_found.4ffb"));
        Assert.state(StrUtil.equals(token, item.getTriggerToken()), I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
        //
        Assert.hasText(item.getSshIds(), I18nMessageUtil.get("i18n.script_not_bound_to_ssh_node.3459"));
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshCommandService.typeName());
        //
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.trigger_token_error_or_expired_with_code.393b"));
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
            log.error(I18nMessageUtil.get("i18n.trigger_auto_execute_ssh_command_template_exception.7451"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.general_execution_exception.62e9") + e.getMessage());
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("batchId", batchId);
        return JsonMessage.success(I18nMessageUtil.get("i18n.start_execution.00d7"), jsonObject);
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
                    String value = I18nMessageUtil.get("i18n.no_data_found.4ffb");
                    jsonObject.put("msg", value);
                    return;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshCommandService.typeName());
                if (userModel == null) {
                    String value = I18nMessageUtil.get("i18n.user_not_exist_trigger_invalid.f375");
                    jsonObject.put("msg", value);
                    return;
                }
                //
                if (!StrUtil.equals(token, item.getTriggerToken())) {
                    String value = I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976");
                    jsonObject.put("msg", value);
                    return;
                }
                BaseServerController.resetInfo(userModel);
                String batchId = null;
                try {
                    batchId = sshCommandService.executeBatch(item, item.getDefParams(), item.getSshIds(), 2);
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.trigger_auto_execute_command_template_exception.4e01"), e);
                    jsonObject.put("msg", I18nMessageUtil.get("i18n.general_execution_exception.62e9") + e.getMessage());
                }
                jsonObject.put("batchId", batchId);
                //
            }).collect(Collectors.toList());
            return JsonMessage.success(I18nMessageUtil.get("i18n.trigger_success.f9d1"), collect);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.ssh_script_batch_trigger_exception.70e1"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.trigger_exception.d624") + e.getMessage());
        }
    }
}
