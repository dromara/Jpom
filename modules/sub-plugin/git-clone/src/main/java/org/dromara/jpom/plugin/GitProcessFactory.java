package org.dromara.jpom.plugin;

import cn.hutool.core.lang.Tuple;

import java.util.Map;

/**
 * GIt执行器
 * <br>
 * Created By Hong on 2023/3/31
 **/
public class GitProcessFactory implements GitProcess {

    private static final String DEFAULT_GIT_PROCESS = "JGit";
    private static final String SYSTEM_GIT_PROCESS = "SystemGit";

    private final GitProcess gitProcess;

    protected GitProcessFactory(Map<String, Object> parameter, IWorkspaceEnvPlugin workspaceEnvPlugin) {
        String processType = (String) parameter.getOrDefault("gitProcessType", DEFAULT_GIT_PROCESS);
        if (SYSTEM_GIT_PROCESS.equalsIgnoreCase(processType) && GitEnv.existsSystemGit()) {
            gitProcess = new SystemGitProcess(workspaceEnvPlugin, parameter);
        } else {
            gitProcess = new JGitProcess(workspaceEnvPlugin, parameter);
        }

    }

    @Override
    public Tuple branchAndTagList() throws Exception {
        return gitProcess.branchAndTagList();
    }

    @Override
    public Object pull() throws Exception {
        return gitProcess.pull();
    }

    @Override
    public Object pullByTag() throws Exception {
        return gitProcess.pullByTag();
    }
}
