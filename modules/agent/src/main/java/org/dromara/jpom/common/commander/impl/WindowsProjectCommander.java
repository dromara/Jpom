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
package org.dromara.jpom.common.commander.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.commander.AbstractProjectCommander;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.common.commander.Commander;
import org.dromara.jpom.common.commander.SystemCommander;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.system.NetstatModel;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.JvmUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

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
                                   SystemCommander systemCommander) {
        super(agentConfig.getProject().getLog().getFileCharset(), systemCommander, agentConfig.getProject());
    }

    @Override
    public String buildJavaCommand(NodeProjectInfoModel nodeProjectInfoModel) {
        String classPath = NodeProjectInfoModel.getClassPathLib(nodeProjectInfoModel);
        if (StrUtil.isBlank(classPath)) {
            return null;
        }
        // 拼接命令
        String jvm = nodeProjectInfoModel.getJvm();
        String tag = nodeProjectInfoModel.getId();
        String mainClass = nodeProjectInfoModel.getMainClass();
        String args = nodeProjectInfoModel.getArgs();
        return StrUtil.format("{} {} {} {} {} {} >> {} &",
            getRunJavaPath(nodeProjectInfoModel, true),
            Optional.ofNullable(jvm).orElse(StrUtil.EMPTY),
            JvmUtil.getJpomPidTag(tag, nodeProjectInfoModel.allLib()),
            classPath,
            Optional.ofNullable(mainClass).orElse(StrUtil.EMPTY),
            Optional.ofNullable(args).orElse(StrUtil.EMPTY),
            nodeProjectInfoModel.getAbsoluteLog());
    }

    @Override
    public CommandOpResult stopJava(NodeProjectInfoModel nodeProjectInfoModel, int pid) {
        String tag = nodeProjectInfoModel.getId();
        List<String> result = new ArrayList<>();
        boolean success = false;
        // 如果正在运行，则执行杀进程命令
        String kill = systemCommander.kill(FileUtil.file(nodeProjectInfoModel.allLib()), pid);
        result.add(kill);
        if (this.loopCheckRun(nodeProjectInfoModel, false)) {
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
}
