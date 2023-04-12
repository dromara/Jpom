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

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Map;

/**
 * GIt执行基类
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 **/
@Slf4j
public abstract class AbstractGitProcess implements GitProcess {

    private final IWorkspaceEnvPlugin workspaceEnvPlugin;
    protected final Map<String, Object> parameter;

    protected AbstractGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        this.workspaceEnvPlugin = workspaceEnvPlugin;
        this.parameter = decryptParameter(parameter);
    }

    /**
     * 解密参数
     *
     * @param parameter 参数
     */
    protected Map<String, Object> decryptParameter(Map<String, Object> parameter) {
        try {
            parameter.put("password", workspaceEnvPlugin.convertRefEnvValue(parameter, "password"));
            parameter.put("username", workspaceEnvPlugin.convertRefEnvValue(parameter, "username"));
        } catch (Exception e) {
            log.error("解密参数失败", e);
        }
        return parameter;
    }


    /**
     * 获取保存路径
     */
    protected File getSaveFile() {
        return (File) parameter.get("savePath");
    }

    /**
     * 获取分支Name
     */
    protected String getBranchName() {
        return (String) parameter.get("branchName");
    }

    /**
     * 获取TagName
     */
    protected String getTagName() {
        return (String) parameter.get("tagName");
    }
}
