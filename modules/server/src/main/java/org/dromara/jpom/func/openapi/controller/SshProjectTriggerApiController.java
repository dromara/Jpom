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
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.model.sshproject.SshProjectStatus;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.sshproject.SshProjectService;
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
 * SSH 项目触发器
 * <p>
 * 与项目、脚本、SSH 脚本等触发器一致：地址为 {@code /api/ssh-project/{id}/{token}?action=start|stop|restart|reload|status}，
 * 不需要登录，但必须持有项目 token；token 由触发器地址接口生成并登记到统一触发器管理，绑定生成人。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@RestController
@RequestMapping
@NotLogin
@Slf4j
public class SshProjectTriggerApiController extends BaseJpomController {

    private final SshProjectService sshProjectService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public SshProjectTriggerApiController(SshProjectService sshProjectService,
                                          TriggerTokenLogServer triggerTokenLogServer) {
        this.sshProjectService = sshProjectService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 触发执行（单条）
     *
     * @param id     项目 id
     * @param token  触发器 token
     * @param action 操作（start/stop/restart/reload/status，默认 status）
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.SSH_PROJECT_TRIGGER_URL, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> trigger(@PathVariable String id,
                                            @PathVariable String token,
                                            String action) {
        SshProjectModel project = this.checkTrigger(id, token);
        UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshProjectService.typeName());
        Assert.notNull(userModel, I18nMessageUtil.get("i18n.trigger_token_error_or_expired_with_code.393b"));
        // 触发器执行上下文记为 token 生成人（执行审计、操作日志口径与其它触发器一致）
        BaseServerController.resetInfo(userModel);
        String useAction = StrUtil.emptyToDefault(action, "status");
        try {
            return this.execAction(project, useAction);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.trigger_auto_execute_server_script_exception.8e84"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.general_execution_exception.62e9") + e.getMessage());
        }
    }

    /**
     * 批量触发
     * <p>
     * 参数 <code>[{"id":"1","token":"a","action":"status"}]</code>
     * <p>
     * 响应 <code>[{"id":"1","token":"a","action":"status","code":200,"data":{},"msg":"..."}]</code>
     *
     * @param request 请求
     * @return json
     */
    @PostMapping(value = ServerOpenApi.SSH_PROJECT_TRIGGER_BATCH, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> triggerBatch(HttpServletRequest request) {
        try {
            String body = ServletUtil.getBody(request);
            JSONArray jsonArray = JSONArray.parseArray(body);
            List<JSONObject> collect = jsonArray.stream().map(o -> {
                JSONObject jsonObject = (JSONObject) o;
                String id = jsonObject.getString("id");
                String token = jsonObject.getString("token");
                String action = StrUtil.emptyToDefault(jsonObject.getString("action"), "status");
                SshProjectModel project = sshProjectService.getByKey(id);
                if (project == null) {
                    jsonObject.put("msg", I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
                    return jsonObject;
                }
                UserModel userModel = triggerTokenLogServer.getUserByToken(token, sshProjectService.typeName());
                if (userModel == null) {
                    jsonObject.put("msg", I18nMessageUtil.get("i18n.user_not_exist_trigger_invalid.f375"));
                    return jsonObject;
                }
                if (!StrUtil.equals(token, project.getTriggerToken())) {
                    jsonObject.put("msg", I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
                    return jsonObject;
                }
                BaseServerController.resetInfo(userModel);
                try {
                    IJsonMessage<JSONObject> message = this.execAction(project, action);
                    jsonObject.put("code", message.getCode());
                    jsonObject.put("msg", message.getMsg());
                    jsonObject.put("data", message.getData());
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.trigger_auto_execute_server_script_exception.8e84"), e);
                    jsonObject.put("code", 500);
                    jsonObject.put("msg", I18nMessageUtil.get("i18n.general_execution_exception.62e9") + e.getMessage());
                }
                return jsonObject;
            }).collect(Collectors.toList());
            return JsonMessage.success(I18nMessageUtil.get("i18n.trigger_success.f9d1"), collect);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.trigger_exception.d624"), e);
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.trigger_exception.d624") + e.getMessage());
        }
    }

    /**
     * 校验项目与 token
     *
     * @param id    项目 id
     * @param token 触发器 token
     * @return 项目
     */
    private SshProjectModel checkTrigger(String id, String token) {
        SshProjectModel project = sshProjectService.getByKey(id);
        Assert.notNull(project, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
        Assert.state(StrUtil.equals(token, project.getTriggerToken()), I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
        return project;
    }

    /**
     * 执行动作：status 走状态探测，其余走生命周期脚本
     *
     * @param project 项目
     * @param action  操作
     * @return json
     */
    private IJsonMessage<JSONObject> execAction(SshProjectModel project, String action) {
        JSONObject result = new JSONObject();
        if (StrUtil.equalsIgnoreCase(action, "status")) {
            SshProjectStatus status = sshProjectService.refreshStatus(project, true);
            result.put("status", status.isRunning() ? "running" : "stopped");
            result.put("pid", status.getPid());
            result.put("port", status.getPorts());
            result.put("message", status.getMessage());
            return JsonMessage.success("", result);
        }
        SshProjectExecuteResult execute = sshProjectService.execute(project, action, null);
        result.put("success", execute.isSuccess());
        result.put("exitCode", execute.getExitCode());
        result.put("message", execute.getMessage());
        result.put("cost", execute.getCost());
        if (!execute.isSuccess()) {
            return new JsonMessage<>(406, StrUtil.emptyToDefault(execute.getMessage(), I18nMessageUtil.get("i18n.ssh_project_exec_fail.4b82")), result);
        }
        return JsonMessage.success("", result);
    }
}
