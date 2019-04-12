package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.thread.GlobalThreadPool;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;

import java.nio.charset.Charset;

/**
 * linux
 *
 * @author Administrator
 */
public class LinuxCommander extends AbstractCommander {

    public LinuxCommander(Charset charset) {
        super(charset);
    }

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
        GlobalThreadPool.execute(() -> execSystemCommand(command));
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
            execCommand(cmd);
            loopCheckRun(projectInfoModel.getId(), false);
            result = status(tag);
        }
        return result;
    }
}
