package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.thread.GlobalThreadPool;
import cn.keepbx.jpom.common.commander.AbstractTomcatCommander;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.util.CommandUtil;

public class LinuxTomcatCommander extends AbstractTomcatCommander {
//    private static final String startCMD = "start.sh";
//    private static final String stopCMD = "shutdown.sh";

    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        // 拼接命令
        String command = String.format("%s/bin/catalina.sh %s", tomcatInfoModel.getPath(), cmd);
        // 执行命令
        GlobalThreadPool.execute(() -> CommandUtil.execSystemCommand(command));
        return "";
    }

    @Override
    public String status() {
        return null;
    }
}
