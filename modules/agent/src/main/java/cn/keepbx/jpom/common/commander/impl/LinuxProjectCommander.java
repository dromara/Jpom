package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.system.NetstatModel;
import cn.keepbx.jpom.util.CommandUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * linux
 *
 * @author Administrator
 */
public class LinuxProjectCommander extends AbstractProjectCommander {

    @Override
    public String start(ProjectInfoModel projectInfoModel) throws Exception {
        String msg = checkStart(projectInfoModel);
        if (msg != null) {
            return msg;
        }
        // 拼接命令
        String command = String.format("nohup java %s %s -Dapplication=%s -Dbasedir=%s %s %s >> %s 2>&1 &",
                projectInfoModel.getJvm(),
                ProjectInfoModel.getClassPathLib(projectInfoModel),
                projectInfoModel.getId(),
                projectInfoModel.getAbsoluteLib(),
                projectInfoModel.getMainClass(),
                projectInfoModel.getArgs(),
                projectInfoModel.getAbsoluteLog());
        //
        GlobalThreadPool.execute(() -> CommandUtil.execSystemCommand(command));
        // 检查是否执行完毕
        loopCheckRun(projectInfoModel.getId(), true);
        return status(projectInfoModel.getId());
    }


    @Override
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String result = super.stop(projectInfoModel);
        String tag = projectInfoModel.getId();
        int pid = parsePid(result);
        if (pid > 0) {
            String cmd = String.format("kill  %s", pid);
            CommandUtil.execCommand(cmd);
            loopCheckRun(projectInfoModel.getId(), false);
            result = status(tag);
        }
        return result;
    }

    @Override
    public List<NetstatModel> listNetstat(int pId) {
        String cmd = "netstat -antup | grep " + pId + " |grep -v \"CLOSE_WAIT\" | head -20";
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
