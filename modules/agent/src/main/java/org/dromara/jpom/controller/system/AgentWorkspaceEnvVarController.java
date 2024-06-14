/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.system;

import cn.hutool.core.map.MapUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.model.system.WorkspaceEnvVarModel;
import org.dromara.jpom.service.system.AgentWorkspaceEnvVarService;
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
    public IJsonMessage<Object> updateWorkspaceEnvVar(@ValidatorItem String name,
                                                      @ValidatorItem String value,
                                                      @ValidatorItem String description,
                                                      Integer privacy) {
        String workspaceId = getWorkspaceId();
        synchronized (AgentWorkspaceEnvVarController.class) {
            WorkspaceEnvVarModel.WorkspaceEnvVarItemModel workspaceEnvVarModel = new WorkspaceEnvVarModel.WorkspaceEnvVarItemModel();
            workspaceEnvVarModel.setName(name);
            workspaceEnvVarModel.setValue(value);
            workspaceEnvVarModel.setDescription(description);
            workspaceEnvVarModel.setPrivacy(privacy);
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
        return JsonMessage.success(I18nMessageUtil.get("i18n.update_success.55aa"));
    }


    /**
     * 删除环境变量
     *
     * @param name 名称
     * @return json
     */
    @PostMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<Object> delete(@ValidatorItem String name) {
        String workspaceId = getWorkspaceId();
        synchronized (AgentWorkspaceEnvVarController.class) {
            //
            WorkspaceEnvVarModel item = agentWorkspaceEnvVarService.getItem(workspaceId);
            if (null != item) {
                item.remove(name);
                agentWorkspaceEnvVarService.updateItem(item);
            }
        }
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }

}
