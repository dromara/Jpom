package cn.keepbx.jpom.common.commander.impl;

import cn.keepbx.jpom.common.commander.Commander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.CommandService;

import java.nio.charset.Charset;

/**
 * linux
 *
 * @author Administrator
 */
public class LinuxCommander extends Commander {

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
        String command = String.format("nohup java %s -classpath %s -Dapplication=%s -Dbasedir=%s %s %s >> %s 2>&1 &",
                projectInfoModel.getJvm(),
                ProjectInfoModel.getClassPathLib(projectInfoModel),
                projectInfoModel.getId(),
                projectInfoModel.getAbsoluteLib(),
                projectInfoModel.getMainClass(),
                projectInfoModel.getArgs(),
                projectInfoModel.getAbsoluteLog());
        execCommand(command);
        Thread.sleep(3000);
        return status(projectInfoModel.getId());
    }


    @Override
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        super.stop(projectInfoModel);
        String tag = projectInfoModel.getId();
        String result = status(tag);
        if (result.startsWith(CommandService.RUNING_TAG)) {
            String pid = result.split(":")[1];
            String cmd = String.format("kill  %s", pid);
            execCommand(cmd);
            Thread.sleep(3000);
            result = status(tag);
        }
        return result;
    }
}
