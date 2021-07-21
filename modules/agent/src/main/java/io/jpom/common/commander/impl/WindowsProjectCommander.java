package io.jpom.common.commander.impl;

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
 * windows 版
 *
 * @author Administrator
 */
public class WindowsProjectCommander extends AbstractProjectCommander {

    @Override
    public String buildCommand(ProjectInfoModel projectInfoModel, ProjectInfoModel.JavaCopyItem javaCopyItem) {
        String classPath = ProjectInfoModel.getClassPathLib(projectInfoModel);
        if (StrUtil.isBlank(classPath)) {
            return null;
        }
        // 拼接命令
        String jvm = javaCopyItem == null ? projectInfoModel.getJvm() : javaCopyItem.getJvm();
        String tag = javaCopyItem == null ? projectInfoModel.getId() : javaCopyItem.getTagId();
        String mainClass = projectInfoModel.getMainClass();
        String args = javaCopyItem == null ? projectInfoModel.getArgs() : javaCopyItem.getArgs();
        return String.format("%s %s %s " +
                        "%s  %s  %s >> %s &",
                getRunJavaPath(projectInfoModel, true),
                jvm, JvmUtil.getJpomPidTag(tag, projectInfoModel.allLib()),
                classPath, mainClass, args, projectInfoModel.getAbsoluteLog(javaCopyItem));
    }

    @Override
    public String stop(ProjectInfoModel projectInfoModel, ProjectInfoModel.JavaCopyItem javaCopyItem) throws Exception {
        String result = super.stop(projectInfoModel, javaCopyItem);
        String tag = javaCopyItem == null ? projectInfoModel.getId() : javaCopyItem.getTagId();
        // 查询状态，如果正在运行，则执行杀进程命令
        int pid = parsePid(result);
        if (pid > 0) {
            String kill = AbstractSystemCommander.getInstance().kill(FileUtil.file(projectInfoModel.allLib()), pid);
            loopCheckRun(projectInfoModel.getId(), false);
            result = status(tag) + StrUtil.SPACE + kill;
        }
        return result;
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
            netstatModel.setLocal(list.get(1));
            netstatModel.setForeign(list.get(2));
            netstatModel.setStatus(list.get(3));
            netstatModel.setName(list.get(4));
            array.add(netstatModel);
        }
        return array;
    }
}
