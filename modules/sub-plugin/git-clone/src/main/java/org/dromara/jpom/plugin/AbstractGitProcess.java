/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

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
            log.error(I18nMessageUtil.get("i18n.decrypt_parameter_failure.d10a"), e);
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
