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
package io.jpom.common.commander.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.model.system.NetstatModel;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * MacOSProjectCommander
 *
 * @author Hotstrip
 * @Description some commands cannot execute success on Mac OS
 */
public class MacOSProjectCommander extends AbstractProjectCommander {

    @Override
    public String buildCommand(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) {
        String path = NodeProjectInfoModel.getClassPathLib(nodeProjectInfoModel);
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String tag = javaCopyItem == null ? nodeProjectInfoModel.getId() : javaCopyItem.getTagId();
        return String.format("nohup %s %s %s" +
                        " %s  %s  %s >> %s 2>&1 &",
                getRunJavaPath(nodeProjectInfoModel, false),
                javaCopyItem == null ? nodeProjectInfoModel.getJvm() : javaCopyItem.getJvm(),
                JvmUtil.getJpomPidTag(tag, nodeProjectInfoModel.allLib()),
                path,
                nodeProjectInfoModel.getMainClass(),
                javaCopyItem == null ? nodeProjectInfoModel.getArgs() : javaCopyItem.getArgs(),
                nodeProjectInfoModel.getAbsoluteLog(javaCopyItem));
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "lsof -n -P -iTCP -sTCP:LISTEN |grep " + pId + " | head -20";
        } else {
            cmd = "lsof -n -P -iTCP -sTCP:CLOSE_WAIT |grep " + pId + " | head -20";
        }
        String result = CommandUtil.execSystemCommand(cmd);
        //DefaultSystemLog.getLog().debug("Mac OS lsof data: {}", result);
        List<String> netList = StrSplitter.splitTrim(result, "\n", true);
        if (netList == null || netList.size() <= 0) {
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
            array.add(netstatModel);
        }
        return array;
    }

    @Override
    public String stop(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        String result = super.stop(nodeProjectInfoModel, javaCopyItem);
        int pid = parsePid(result);
        if (pid > 0) {
            String kill = AbstractSystemCommander.getInstance().kill(FileUtil.file(nodeProjectInfoModel.allLib()), pid);
            if (loopCheckRun(nodeProjectInfoModel.getId(), false)) {
                // 强制杀进程
                String cmd = String.format("kill -9 %s", pid);
                CommandUtil.asyncExeLocalCommand(FileUtil.file(nodeProjectInfoModel.allLib()), cmd);
            }
            String tag = javaCopyItem == null ? nodeProjectInfoModel.getId() : javaCopyItem.getTagId();
            result = status(tag) + StrUtil.SPACE + kill;
        }
        return result;
    }
}
