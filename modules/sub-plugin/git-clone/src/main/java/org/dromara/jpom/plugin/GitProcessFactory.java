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

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.util.CommandUtil;

import java.util.Map;

/**
 * GIt执行器
 * <br>
 * Created By Hong on 2023/3/31
 *
 * @author Hong
 **/
@Slf4j
public class GitProcessFactory {

    private static final String WIN_EXISTS_GIT = "where git";
    private static final String LINUX_EXISTS_GIT = "which git";

    private static Boolean result;

    private static final String DEFAULT_GIT_PROCESS = "JGit";
    private static final String SYSTEM_GIT_PROCESS = "SystemGit";

    public static GitProcess get(Map<String, Object> parameter, IWorkspaceEnvPlugin workspaceEnvPlugin) {
        String processType = (String) parameter.getOrDefault("gitProcessType", DEFAULT_GIT_PROCESS);
        if (SYSTEM_GIT_PROCESS.equalsIgnoreCase(processType) && GitProcessFactory.existsSystemGit()) {
            return new SystemGitProcess(workspaceEnvPlugin, parameter);
        } else {
            return new JGitProcess(workspaceEnvPlugin, parameter);
        }
    }


    /**
     * 操作系统是否有GIT环境
     */
    public static boolean existsSystemGit() {
        if (result == null) {
            result = existsSystemGit2();
        }
        return result;
    }

    /**
     * 操作系统是否有GIT环境
     */
    private static boolean existsSystemGit2() {
        String result;
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isWindows()) {
            result = CommandUtil.execSystemCommand(WIN_EXISTS_GIT);
            if (StrUtil.contains(result, ".exe")) {
                log.info(I18nMessageUtil.get("i18n.git_installation_location.7984"), result);
                return true;
            }
        } else if (osInfo.isLinux() || osInfo.isMac()) {
            result = CommandUtil.execSystemCommand(LINUX_EXISTS_GIT);
            if (StrUtil.containsAny(result, "no git", "not found")) {
                return false;
            }
            log.info(I18nMessageUtil.get("i18n.git_installation_location.7984"), result);
            return true;
        } else {
            log.warn(I18nMessageUtil.get("i18n.unsupported_system_type_with_placeholder.d5cc"), osInfo.getName());
            return false;
        }
        return false;
    }

}
