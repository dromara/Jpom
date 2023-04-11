package org.dromara.jpom.plugin;

import cn.hutool.core.lang.Tuple;

import java.io.PrintWriter;
import java.util.Map;

/**
 * JGit操作
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 **/
public class JGitProcess extends AbstractGitProcess {

    protected JGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        super(workspaceEnvPlugin, parameter);
    }

    @Override
    public Tuple branchAndTagList() throws Exception {
        return JGitUtil.getBranchAndTagList(parameter);
    }

    @Override
    public String[] pull() throws Exception {
        PrintWriter printWriter = (PrintWriter) parameter.get("logWriter");
        return JGitUtil.checkoutPull(parameter, getSaveFile(), getBranchName(), printWriter);
    }

    @Override
    public String[] pullByTag() throws Exception {
        PrintWriter printWriter = (PrintWriter) parameter.get("logWriter");
        return JGitUtil.checkoutPullTag(parameter, getSaveFile(), getTagName(), printWriter);
    }
}
