package io.jpom.plugin;

import io.jpom.common.Const;

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
        String workspaceId = (String) parameter.get(Const.WORKSPACEID_REQ_HEADER);
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
