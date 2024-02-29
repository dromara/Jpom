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

import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.commander.AbstractProjectCommander;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.common.commander.Commander;
import org.dromara.jpom.common.commander.SystemCommander;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.system.NetstatModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.script.DslScriptServer;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.JvmUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * windows 版
 *
 * @author Administrator
 */
@Conditional(Commander.Windows.class)
@Service
public class WindowsProjectCommander extends AbstractProjectCommander {

    public WindowsProjectCommander(AgentConfig agentConfig,
                                   SystemCommander systemCommander,
                                   DslScriptServer dslScriptServer,
                                   ProjectInfoService projectInfoService) {
        super(agentConfig.getProject().getLog().getFileCharset(), systemCommander, agentConfig.getProject(), dslScriptServer, projectInfoService);
    }

    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(nodeProjectInfoModel);
        return this.buildRunCommand(nodeProjectInfoModel, infoModel);
    }

    @Override
    public String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String lib = projectInfoService.resolveLibPath(originalModel);
        String classPath = this.getClassPathLib(originalModel, lib);
        if (StrUtil.isBlank(classPath)) {
            return null;
        }
        // 拼接命令
        String jvm = nodeProjectInfoModel.getJvm();
        String tag = nodeProjectInfoModel.getId();
        String mainClass = originalModel.mainClass();
        String args = nodeProjectInfoModel.getArgs();

        String absoluteLog = projectInfoService.resolveAbsoluteLog(nodeProjectInfoModel, originalModel);
        return StrUtil.format("{} {} {} {} {} {} >> {} &",
            getRunJavaPath(nodeProjectInfoModel, true),
            Optional.ofNullable(jvm).orElse(StrUtil.EMPTY),
            JvmUtil.getJpomPidTag(tag, lib),
            classPath,
            Optional.ofNullable(mainClass).orElse(StrUtil.EMPTY),
            Optional.ofNullable(args).orElse(StrUtil.EMPTY),
            absoluteLog);
    }

    @Override
    public CommandOpResult stopJava(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel, int pid) {
        String tag = nodeProjectInfoModel.getId();
        List<String> result = new ArrayList<>();
        boolean success = false;
        // 如果正在运行，则执行杀进程命令
        File file = projectInfoService.resolveLibFile(nodeProjectInfoModel);
        String kill = systemCommander.kill(file, pid);
        result.add(kill);
        if (this.loopCheckRun(nodeProjectInfoModel, originalModel, false)) {
            success = true;
        } else {
            result.add("Kill not completed");
        }
        return CommandOpResult.of(success, status(tag)).appendMsg(result);
        // return status(tag) + StrUtil.SPACE + kill;
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "netstat -nao -p tcp | findstr \"LISTENING\" | findstr " + pId;
        } else {
            cmd = "netstat -nao -p tcp | findstr /V \"CLOSE_WAIT\" | findstr " + pId;
        }
        String result = CommandUtil.execSystemCommand(cmd);
        List<String> netList = StrSplitter.splitTrim(result, StrUtil.LF, true);
        if (netList == null || netList.isEmpty()) {
            return null;
        }
        List<NetstatModel> array = new ArrayList<>();
        for (String str : netList) {
            List<String> list = StrSplitter.splitTrim(str, " ", true);
            if (list.size() < 5) {
                continue;
            }
            NetstatModel netstatModel = new NetstatModel();
            netstatModel.setProtocol(list.get(0));
            netstatModel.setLocal(list.get(1));
            netstatModel.setForeign(list.get(2));
            netstatModel.setStatus(list.get(3));
            netstatModel.setName(list.get(4));
            array.add(netstatModel);
        }
        return array;
    }

    // tasklist | findstr /s /i "java"
    // wmic process where caption="javaw.exe" get processid,caption,commandline /value
}
