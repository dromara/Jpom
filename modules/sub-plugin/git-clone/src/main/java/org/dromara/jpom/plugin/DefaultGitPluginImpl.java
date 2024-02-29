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

import cn.keepbx.jpom.plugins.PluginConfig;
import org.eclipse.jgit.api.Git;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/22
 */
@PluginConfig(name = "git-clone")
public class DefaultGitPluginImpl implements IWorkspaceEnvPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        String type = main.toString();
        GitProcess gitProcess = GitProcessFactory.get(parameter, this);
        switch (type) {
            case "branchAndTagList":
                return gitProcess.branchAndTagList();
            case "pull": {
                return gitProcess.pull();
            }
            case "pullByTag": {
                return gitProcess.pullByTag();
            }
            case "systemGit": {
                return GitProcessFactory.existsSystemGit();
            }
            default:
                break;
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        Git.shutdown();
    }
}
