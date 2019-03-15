package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.util.CharsetUtil;
import cn.keepbx.jpom.common.commander.Commander;
import cn.keepbx.jpom.model.ProjectInfoModel;

import java.io.File;

public class WindowsCommander extends Commander {

    public WindowsCommander() {
        charset = CharsetUtil.CHARSET_GBK;
    }

    // 启动
    @Override
    public String start(ProjectInfoModel projectInfoModel) throws Exception {
        String result;

        // 拼接命令
        String jvm = projectInfoModel.getJvm();
        String tag = projectInfoModel.getId();
        String lib = projectInfoModel.getLib();
        String mainClass = projectInfoModel.getMainClass();
        String args = projectInfoModel.getArgs();
        String log = projectInfoModel.getLog();
        String classPath = "";
        File fileLib = new File(lib);
        File[] files = fileLib.listFiles();

        // 获取lib下面的所有jar包
        for (File file : files) {
            classPath += file.getPath() + ";";
        }

        String command = String.format("javaw %s -classpath %s -Dapplication=%s -Dbasedir=%s %s %s >> %s", jvm, classPath, tag, lib, mainClass, args, log);
        System.out.println("===>>" + command);
        // 执行命令
        Runtime.getRuntime().exec(command);
        Thread.sleep(3000);
        result = status(projectInfoModel.getId());

        return result;
    }

    // 停止
    @Override
    public String stop(String tag) throws Exception {
        // 查询状态，如果正在运行，则执行杀进程命令
        String result = status(tag);
        if (result.startsWith("running")) {
            String pid = result.split(":")[1];
            String cmd = String.format("taskkill /F /PID %s", pid);
            Runtime.getRuntime().exec(cmd);
            Thread.sleep(3000);
            result = status(tag);
        }

        return result;
    }

    // 重启
    @Override
    public String restart(ProjectInfoModel projectInfoModel) throws Exception {
        String result = status(projectInfoModel.getId());

        // 如果正在运行，需要先停止
        if (result.startsWith("running")) {
            stop(projectInfoModel.getId());
        }
        result = start(projectInfoModel);
        return result;
    }
}
