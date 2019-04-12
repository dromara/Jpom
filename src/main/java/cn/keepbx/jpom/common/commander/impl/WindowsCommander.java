package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.thread.GlobalThreadPool;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.NetstatModel;
import cn.keepbx.jpom.model.ProjectInfoModel;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * windows 版
 *
 * @author Administrator
 */
public class WindowsCommander extends AbstractCommander {

    public WindowsCommander(Charset charset) {
        super(charset);
    }

    @Override
    public String start(ProjectInfoModel projectInfoModel) throws Exception {
        String msg = checkStart(projectInfoModel);
        if (msg != null) {
            return msg;
        }
        // 拼接命令
        String jvm = projectInfoModel.getJvm();
        String tag = projectInfoModel.getId();
        String mainClass = projectInfoModel.getMainClass();
        String args = projectInfoModel.getArgs();
        String classPath = ProjectInfoModel.getClassPathLib(projectInfoModel);

        String command = String.format("javaw %s %s -Dapplication=%s -Dbasedir=%s %s %s >> %s &",
                jvm, classPath, tag,
                projectInfoModel.getAbsoluteLib(), mainClass, args, projectInfoModel.getAbsoluteLog());
        // 执行命令
        GlobalThreadPool.execute(() -> execSystemCommand(command));
        //
        loopCheckRun(projectInfoModel.getId(), true);
        return status(projectInfoModel.getId());
    }

    @Override
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String result = super.stop(projectInfoModel);
        String tag = projectInfoModel.getId();
        // 查询状态，如果正在运行，则执行杀进程命令
        int pid = parsePid(result);
        if (pid > 0) {
            String cmd = String.format("taskkill /F /PID %s", pid);
            execCommand(cmd);
            loopCheckRun(projectInfoModel.getId(), false);
            result = status(tag);
        }
        return result;
    }

    @Override
    public List<NetstatModel> listNetstat(int pId) {
        String cmd = "netstat -nao -p tcp | findstr /V \"CLOSE_WAIT\" | findstr " + pId;
        String result = execSystemCommand(cmd);
        List<String> netList = StrSpliter.splitTrim(result, StrUtil.LF, true);
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
            netstatModel.setLocal(list.get(1));
            netstatModel.setForeign(list.get(2));
            netstatModel.setStatus(list.get(3));
            netstatModel.setName(list.get(4));
            array.add(netstatModel);
        }
        return array;
    }
}
