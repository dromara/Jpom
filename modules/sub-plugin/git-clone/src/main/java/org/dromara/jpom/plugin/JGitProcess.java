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
