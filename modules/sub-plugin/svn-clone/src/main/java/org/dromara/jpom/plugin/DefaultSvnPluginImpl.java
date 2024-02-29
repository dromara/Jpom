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

import java.io.File;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/12/23
 */
@PluginConfig(name = "svn-clone")
public class DefaultSvnPluginImpl implements IWorkspaceEnvPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        File savePath = (File) main;
        // 转化密码字段
        parameter.put("password", convertRefEnvValue(parameter, "password"));
        parameter.put("username", convertRefEnvValue(parameter, "username"));
        return SvnKitUtil.checkOut(parameter, savePath);
    }
}
