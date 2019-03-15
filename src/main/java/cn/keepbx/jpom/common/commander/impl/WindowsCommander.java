package cn.keepbx.jpom.common.commander.impl;

import cn.keepbx.jpom.common.commander.Commander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.CommandService;

import java.nio.charset.Charset;

/**
 * windows 版
 *
 * @author Administrator
 */
public class WindowsCommander extends Commander {

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

        String command = String.format("javaw %s -classpath %s -Dapplication=%s -Dbasedir=%s %s %s >> %s",
                jvm, classPath, tag,
                projectInfoModel.getAbsoluteLib(), mainClass, args, projectInfoModel.getAbsoluteLog());
        // 执行命令;
        asyncExec(command);
        //
        Thread.sleep(3000);
        return status(projectInfoModel.getId());
    }

    @Override
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        super.stop(projectInfoModel);
        String tag = projectInfoModel.getId();
        // 查询状态，如果正在运行，则执行杀进程命令
        String result = status(tag);
        if (result.startsWith(CommandService.RUNING_TAG)) {
            String pid = result.split(":")[1];
            String cmd = String.format("taskkill /F /PID %s", pid);
            execCommand(cmd);
            Thread.sleep(3000);
            result = status(tag);
        }
        return result;
    }
}
