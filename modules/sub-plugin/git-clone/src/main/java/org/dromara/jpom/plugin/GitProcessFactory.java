package org.dromara.jpom.plugin;

import java.util.Map;

/**
 * GIt执行器
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 **/
public class GitProcessFactory {

    private static final String DEFAULT_GIT_PROCESS = "JGit";
    private static final String SYSTEM_GIT_PROCESS = "SystemGit";

    public static GitProcess get(Map<String, Object> parameter, IWorkspaceEnvPlugin workspaceEnvPlugin) {
        String processType = (String) parameter.getOrDefault("gitProcessType", DEFAULT_GIT_PROCESS);
        if (SYSTEM_GIT_PROCESS.equalsIgnoreCase(processType) && GitEnv.existsSystemGit()) {
            return new SystemGitProcess(workspaceEnvPlugin, parameter);
        } else {
            return new JGitProcess(workspaceEnvPlugin, parameter);
        }
    }
}
