/*
 * Copyright (c) 2019 Of Him Code Technology Studio
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
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.commander.BaseUnixProjectCommander;
import org.dromara.jpom.common.commander.Commander;
import org.dromara.jpom.common.commander.SystemCommander;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.model.system.NetstatModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.script.DslScriptServer;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * linux
 *
 * @author bwcx_jzy
 */
@Conditional(Commander.Linux.class)
@Service
@Primary
@Slf4j
public class LinuxProjectCommander extends BaseUnixProjectCommander {

    public LinuxProjectCommander(AgentConfig agentConfig,
                                 SystemCommander systemCommander,
                                 DslScriptServer dslScriptServer,
                                 ProjectInfoService projectInfoService) {
        super(systemCommander, agentConfig.getProject(), dslScriptServer, projectInfoService);
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "netstat -antup | grep " + pId + " |grep \"LISTEN\" | head -20";
        } else {
            cmd = "netstat -antup | grep " + pId + " |grep -v \"CLOSE_WAIT\" | head -20";
        }
        return this.listNetstat(cmd);
    }

    protected List<NetstatModel> listNetstat(String cmd) {
        String result = CommandUtil.execSystemCommand(cmd);
        List<String> netList = StrSplitter.splitTrim(result, StrUtil.LF, true);
        if (CollUtil.isEmpty(netList)) {
            return null;
        }
        return netList.stream().map(str -> {
            List<String> list = StrSplitter.splitTrim(str, " ", true);
            if (list.size() < 5) {
                return null;
            }
            NetstatModel netstatModel = new NetstatModel();
            netstatModel.setProtocol(list.get(0));
            netstatModel.setReceive(list.get(1));
            netstatModel.setSend(list.get(2));
            netstatModel.setLocal(list.get(3));
            netstatModel.setForeign(list.get(4));
            if ("tcp".equalsIgnoreCase(netstatModel.getProtocol())) {
                netstatModel.setStatus(CollUtil.get(list, 5));
                netstatModel.setName(CollUtil.get(list, 6));
            } else {
                netstatModel.setStatus(StrUtil.DASHED);
                netstatModel.setName(CollUtil.get(list, 5));
            }

            return netstatModel;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
