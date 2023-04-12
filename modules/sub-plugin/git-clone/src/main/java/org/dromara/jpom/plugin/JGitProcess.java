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
