package io.jpom.plugin;

import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.Const;
import io.jpom.service.system.WorkspaceEnvVarService;

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
        String workspaceId = (String) parameter.get(Const.WORKSPACEID_REQ_HEADER);
        String value = (String) parameter.get("value");
        return workspaceEnvVarService.convertRefEnvValue(workspaceId, value);
    }
}
