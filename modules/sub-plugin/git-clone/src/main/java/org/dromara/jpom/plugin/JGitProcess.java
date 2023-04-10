package org.dromara.jpom.plugin;

import cn.hutool.core.lang.Tuple;

import java.io.PrintWriter;
import java.util.Map;

/**
 * JGit操作
 * <br>
 * Created By Hong on 2023/3/31
 **/
public class JGitProcess extends AbstractGitProcess {

    protected JGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        super(workspaceEnvPlugin, parameter);
    }

    @Override
    public Tuple branchAndTagList() throws Exception {
        return JGitUtil.getBranchAndTagList(getParameter());
    }

    @Override
    public Object pull() throws Exception {
        Map<String, Object> map = getParameter();
        PrintWriter printWriter = (PrintWriter) map.get("logWriter");
        return JGitUtil.checkoutPull(map, getSaveFile(), getBranchName(), printWriter);
    }

    @Override
    public Object pullByTag() throws Exception {
        Map<String, Object> map = getParameter();
        PrintWriter printWriter = (PrintWriter) getParameter().get("logWriter");
        return JGitUtil.checkoutPullTag(map, getSaveFile(), getTagName(), printWriter);
    }
}
