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

    public EnvironmentMapBuilder getEnv(String workspaceId) {
        WorkspaceEnvVarModel item = this.getItem(workspaceId);
        Map<String, EnvironmentMapBuilder.Item> objectMap = Optional.ofNullable(item)
            .map(WorkspaceEnvVarModel::getVarData)
            .map(map -> CollStreamUtil.toMap(map.values(), WorkspaceEnvVarModel.WorkspaceEnvVarItemModel::getName, workspaceEnvVarItemModel -> {
                // 需要考虑兼容之前没有隐私变量字段，默认为隐私字段
                Integer privacy = workspaceEnvVarItemModel.getPrivacy();
                return new EnvironmentMapBuilder.Item(workspaceEnvVarItemModel.getName(), privacy == null || privacy == 1);
            })).orElse(new HashMap<>(1));
        return EnvironmentMapBuilder.builder(objectMap);
    }
}
