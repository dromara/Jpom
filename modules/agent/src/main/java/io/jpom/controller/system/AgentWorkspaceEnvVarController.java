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
package io.jpom.controller.system;

import cn.hutool.core.map.MapUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import io.jpom.common.BaseAgentController;
import io.jpom.model.system.WorkspaceEnvVarModel;
import io.jpom.service.system.AgentWorkspaceEnvVarService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lidaofu
 * @since 2022/3/8
 */
@RestController
@RequestMapping(value = "/system/workspace_env")
public class AgentWorkspaceEnvVarController extends BaseAgentController {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;

    public AgentWorkspaceEnvVarController(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService) {
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
    }

    /**
     * 更新环境变量
     *
     * @param name        名称
     * @param value       值
     * @param description 描述
     * @return json
     */
    @PostMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public String updateWorkspaceEnvVar(@ValidatorItem String name,
                                        @ValidatorItem String value,
                                        @ValidatorItem String description) {
        String workspaceId = getWorkspaceId();
        synchronized (AgentWorkspaceEnvVarController.class) {
            WorkspaceEnvVarModel.WorkspaceEnvVarItemModel workspaceEnvVarModel = new WorkspaceEnvVarModel.WorkspaceEnvVarItemModel();
            workspaceEnvVarModel.setName(name);
            workspaceEnvVarModel.setValue(value);
            workspaceEnvVarModel.setDescription(description);
            //
            WorkspaceEnvVarModel item = agentWorkspaceEnvVarService.getItem(workspaceId);
            if (null == item) {
                item = new WorkspaceEnvVarModel();
                item.setVarData(MapUtil.of(name, workspaceEnvVarModel));
                item.setName(workspaceId);
                item.setId(workspaceId);
                agentWorkspaceEnvVarService.addItem(item);
            } else {
                item.put(name, workspaceEnvVarModel);
                agentWorkspaceEnvVarService.updateItem(item);
            }
        }
        return JsonMessage.getString(200, "更新成功");
    }


    /**
     * 删除环境变量
     *
     * @param name 名称
     * @return json
     */
    @PostMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public String delete(@ValidatorItem String name) {
        String workspaceId = getWorkspaceId();
        synchronized (AgentWorkspaceEnvVarController.class) {
            //
            WorkspaceEnvVarModel item = agentWorkspaceEnvVarService.getItem(workspaceId);
            if (null != item) {
                item.remove(name);
                agentWorkspaceEnvVarService.updateItem(item);
            }
        }
        return JsonMessage.getString(200, "删除成功");
    }

}
