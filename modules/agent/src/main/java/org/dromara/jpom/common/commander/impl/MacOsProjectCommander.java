/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.commander.BaseUnixProjectCommander;
import org.dromara.jpom.common.commander.Commander;
import org.dromara.jpom.common.commander.SystemCommander;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.model.system.NetstatModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.script.DslScriptServer;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * MacOSProjectCommander
 * <p>
 * some commands cannot execute success on Mac OS
 *
 * @author Hotstrip
 */
@Conditional(Commander.Mac.class)
@Service
public class MacOsProjectCommander extends BaseUnixProjectCommander {

    public MacOsProjectCommander(AgentConfig agentConfig,
                                 SystemCommander systemCommander,
                                 DslScriptServer dslScriptServer,
                                 ProjectInfoService projectInfoService) {
        super(agentConfig.getProject().getLog().getFileCharset(), systemCommander, agentConfig.getProject(), dslScriptServer, projectInfoService);
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "lsof -n -P -iTCP -sTCP:LISTEN |grep " + pId + " | head -20";
        } else {
            cmd = "lsof -n -P -iTCP -sTCP:CLOSE_WAIT |grep " + pId + " | head -20";
        }
        return this.listNetstat(cmd);
    }

    protected List<NetstatModel> listNetstat(String cmd) {
        String result = CommandUtil.execSystemCommand(cmd);
        List<String> netList = StrSplitter.splitTrim(result, StrUtil.LF, true);
        if (CollUtil.isEmpty(netList)) {
            return null;
        }
        return netList.stream()
            .map(str -> {
                List<String> list = StrSplitter.splitTrim(str, " ", true);
                if (list.size() < 10) {
                    return null;
                }
                NetstatModel netstatModel = new NetstatModel();
                netstatModel.setProtocol(list.get(7));
                //netstatModel.setReceive(list.get(1));
                //netstatModel.setSend(list.get(2));
                netstatModel.setLocal(list.get(8));
                netstatModel.setForeign(list.get(4));
                if ("tcp".equalsIgnoreCase(netstatModel.getProtocol())) {
                    netstatModel.setStatus(CollUtil.get(list, 9));
                    netstatModel.setName(CollUtil.get(list, 0));
                } else {
                    netstatModel.setStatus(StrUtil.DASHED);
                    netstatModel.setName(CollUtil.get(list, 5));
                }

                return netstatModel;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
