/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.plugins.PluginConfig;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/8/30
 */
@PluginConfig(name = IWorkspaceEnvPlugin.PLUGIN_NAME)
public class DefaultWorkspaceEnvPlugin implements IWorkspaceEnvPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        WorkspaceEnvVarService workspaceEnvVarService = SpringUtil.getBean(WorkspaceEnvVarService.class);
        String workspaceId = (String) parameter.get(Const.WORKSPACE_ID_REQ_HEADER);
        String value = (String) parameter.get("value");
        return workspaceEnvVarService.convertRefEnvValue(workspaceId, value);
    }
}
