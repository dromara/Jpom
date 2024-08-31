/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.system;

import cn.hutool.core.collection.CollStreamUtil;
import org.dromara.jpom.common.AgentConst;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.system.WorkspaceEnvVarModel;
import org.dromara.jpom.service.BaseOperService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author lidaofu
 * @since 2022/3/8 9:16
 **/
@Service
public class AgentWorkspaceEnvVarService extends BaseOperService<WorkspaceEnvVarModel> {

    public AgentWorkspaceEnvVarService() {
        super(AgentConst.WORKSPACE_ENV_VAR);
    }

    /**
     * 获取指定工作空间的环境变量
     *
     * @param workspaceId 工作空间
     * @return env
     */
    public EnvironmentMapBuilder getEnv(String workspaceId) {
        WorkspaceEnvVarModel item = this.getItem(workspaceId);
        Map<String, EnvironmentMapBuilder.Item> objectMap = Optional.ofNullable(item)
            .map(WorkspaceEnvVarModel::getVarData)
            .map(map -> CollStreamUtil.toMap(map.values(), WorkspaceEnvVarModel.WorkspaceEnvVarItemModel::getName, workspaceEnvVarItemModel -> {
                // 需要考虑兼容之前没有隐私变量字段，默认为隐私字段
                Integer privacy = workspaceEnvVarItemModel.getPrivacy();
                return new EnvironmentMapBuilder.Item(workspaceEnvVarItemModel.getValue(), privacy == null || privacy == 1, false);
            }))
            .orElse(new HashMap<>(1));
        return EnvironmentMapBuilder.builder(objectMap);
    }
}
