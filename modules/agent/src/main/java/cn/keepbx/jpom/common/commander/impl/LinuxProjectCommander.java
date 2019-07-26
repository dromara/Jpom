package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.system.NetstatModel;
import cn.keepbx.util.CommandUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * linux
 *
 * @author Administrator
 */
public class LinuxProjectCommander extends AbstractProjectCommander {

    @Override
    public String buildCommand(ProjectInfoModel projectInfoModel) {
        String path = ProjectInfoModel.getClassPathLib(projectInfoModel);
        if (StrUtil.isBlank(path)) {
            return null;
        }
        // 拼接命令
        return String.format("nohup java %s %s -Dapplication=%s -Dbasedir=%s %s %s >> %s 2>&1 &",
                projectInfoModel.getJvm(),
                path,
                projectInfoModel.getId(),
                projectInfoModel.getAbsoluteLib(),
                projectInfoModel.getMainClass(),
                projectInfoModel.getArgs(),
                projectInfoModel.getAbsoluteLog());
    }

    @Override
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String result = super.stop(projectInfoModel);
        String tag = projectInfoModel.getId();
        int pid = parsePid(result);
        if (pid > 0) {
            String cmd = String.format("kill  %s", pid);
            CommandUtil.asyncExeLocalCommand(FileUtil.file(projectInfoModel.getLib()), cmd);
            if (loopCheckRun(projectInfoModel.getId(), false)) {
                // 强制杀进程
                cmd = String.format("kill -9 %s", pid);
                CommandUtil.asyncExeLocalCommand(FileUtil.file(projectInfoModel.getLib()), cmd);
            }
            result = status(tag);
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
        List<String> netList = StrSpliter.splitTrim(result, "\n", true);
        if (netList == null || netList.size() <= 0) {
            return null;
        }
        List<NetstatModel> array = new ArrayList<>();
        for (String str : netList) {
            List<String> list = StrSpliter.splitTrim(str, " ", true);
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
                netstatModel.setStatus(list.get(5));
                netstatModel.setName(list.get(6));
            } else {
                netstatModel.setStatus(StrUtil.DASHED);
                netstatModel.setName(list.get(5));
            }
            array.add(netstatModel);
        }
        return array;
    }
}
