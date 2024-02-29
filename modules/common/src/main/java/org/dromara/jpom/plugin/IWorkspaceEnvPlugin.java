/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import org.dromara.jpom.common.Const;

import java.util.Map;

/**
 * 工作空间环境变量 插件
 *
 * @author bwcx_jzy
 * @since 2022/8/30
 */
public interface IWorkspaceEnvPlugin extends IDefaultPlugin {

    String PLUGIN_NAME = "workspace-Env";

    /**
     * 转化 工作空间环境变量
     *
     * @param parameter 插件的参数
     * @param key       参数中的key
     * @return 转化后的
     * @throws Exception 异常
     */
    default String convertRefEnvValue(Map<String, Object> parameter, String key) throws Exception {
        String workspaceId = (String) parameter.get(Const.WORKSPACE_ID_REQ_HEADER);
        return this.convertRefEnvValue(workspaceId, (String) parameter.get(key));
    }

    /**
     * 转化 工作空间环境变量
     *
     * @param workspaceId 工作空间
     * @param value       值
     * @return 如果存在值，则返回环境变量值。不存在则返回原始值
     * @throws Exception 异常
     */
    default String convertRefEnvValue(String workspaceId, String value) throws Exception {
        return (String) PluginFactory.getPlugin(PLUGIN_NAME).execute("convert", "workspaceId", workspaceId, "value", value);
    }
}
