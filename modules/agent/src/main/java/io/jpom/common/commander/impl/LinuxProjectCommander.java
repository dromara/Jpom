package io.jpom.common.commander.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.model.system.NetstatModel;
import io.jpom.util.CommandUtil;
import io.jpom.util.JvmUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * linux
 *
 * @author Administrator
 */
public class LinuxProjectCommander extends AbstractProjectCommander {

    @Override
    public String buildCommand(ProjectInfoModel projectInfoModel, ProjectInfoModel.JavaCopyItem javaCopyItem) {
        String path = ProjectInfoModel.getClassPathLib(projectInfoModel);
        if (StrUtil.isBlank(path)) {
            return null;
        }
        String tag = javaCopyItem == null ? projectInfoModel.getId() : javaCopyItem.getTagId();
        return String.format("nohup %s %s %s" +
                        " %s  %s  %s >> %s 2>&1 &",
                getRunJavaPath(projectInfoModel, false),
                javaCopyItem == null ? projectInfoModel.getJvm() : javaCopyItem.getJvm(),
                JvmUtil.getJpomPidTag(tag, projectInfoModel.allLib()),
                path,
                projectInfoModel.getMainClass(),
                javaCopyItem == null ? projectInfoModel.getArgs() : javaCopyItem.getArgs(),
                projectInfoModel.getAbsoluteLog(javaCopyItem));
    }

    @Override
    public String stop(ProjectInfoModel projectInfoModel, ProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        String result = super.stop(projectInfoModel, javaCopyItem);
        int pid = parsePid(result);
        if (pid > 0) {
            String kill = AbstractSystemCommander.getInstance().kill(FileUtil.file(projectInfoModel.allLib()), pid);
            if (loopCheckRun(projectInfoModel.getId(), false)) {
                // 强制杀进程
                String cmd = String.format("kill -9 %s", pid);
                CommandUtil.asyncExeLocalCommand(FileUtil.file(projectInfoModel.allLib()), cmd);
            }
            String tag = javaCopyItem == null ? projectInfoModel.getId() : javaCopyItem.getTagId();
            result = status(tag) + StrUtil.SPACE + kill;
        }
        return result;
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "netstat -antup | grep " + pId + " |grep \"LISTEN\" | head -20";
        } else {
            cmd = "netstat -antup | grep " + pId + " |grep -v \"CLOSE_WAIT\" | head -20";
        }
        String result = CommandUtil.execSystemCommand(cmd);
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
}
