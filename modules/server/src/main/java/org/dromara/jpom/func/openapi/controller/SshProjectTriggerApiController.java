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
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseJpomController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.interceptor.NotLogin;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.service.sshproject.SshProjectService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SSH 项目触发器
 * <p>
 * 地址形如 `/openapi/ssh-project/{id}/{token}/?action=start`，token 与项目配置一致；
 * 与现有项目触发器一致：不需要登录，但必须持有项目 token。
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

    public SshProjectTriggerApiController(SshProjectService sshProjectService) {
        this.sshProjectService = sshProjectService;
    }

    @RequestMapping(value = "/openapi/ssh-project/{id}/{token}/", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> trigger(@PathVariable String id,
                                            @PathVariable String token,
                                            String action) {
        SshProjectModel project = sshProjectService.getByKey(id);
        Assert.notNull(project, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
        Assert.state(StrUtil.equals(token, project.getToken()), I18nMessageUtil.get("i18n.trigger_token_error_or_expired.8976"));
        String useAction = StrUtil.emptyToDefault(action, "status");
        JSONObject result = new JSONObject();
        if (StrUtil.equalsIgnoreCase(useAction, "status")) {
            org.dromara.jpom.model.sshproject.SshProjectStatus status = sshProjectService.refreshStatus(project, true);
            result.put("status", status.isRunning() ? "running" : "stopped");
            result.put("pid", status.getPid());
            result.put("port", status.getPorts());
            result.put("message", status.getMessage());
            return JsonMessage.success("", result);
        }
        SshProjectExecuteResult execute = sshProjectService.execute(project, useAction, null);
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
