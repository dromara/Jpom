package io.jpom.common.commander.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.commander.AbstractTomcatCommander;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.util.CommandUtil;

import java.util.List;

/**
 * tomcat的Windows管理命令
 *
 * @author LF
 */
public class WindowsTomcatCommander extends AbstractTomcatCommander {

    /**
     * windows下执行tomcat命令
     *
     * @param tomcatInfoModel tomcat信息
     * @param cmd             执行的命令，包括start stop
     * @return 返回tomcat启动结果
     */
    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        String tomcatPath = tomcatInfoModel.pathAndCheck();
        //截取盘符
        String dcPath = null;
        if (tomcatPath != null && tomcatPath.indexOf("/") > 1) {
            dcPath = tomcatPath.substring(0, tomcatPath.indexOf("/"));
        }
        String command = null;
        if (StrUtil.isBlank(tomcatPath)) {
            return "tomcat path blank";
        }

        if (cmd.equals("stop")) {
            String setPidCmd = CommandUtil.execSystemCommand("jps -mv");
            List<String> list = StrSplitter.splitTrim(setPidCmd, StrUtil.LF, true);
            for (String item : list) {
                //window下路径格式转换
                String msg = FileUtil.normalize(item + "/");
                //判断集合中元素是否包含指定Tomcat路径
                boolean w = msg.contains(tomcatInfoModel.getPath());
                if (w) {
                    //截取TomcatPid
                    if (msg.indexOf(" ") > 1) {
                        String tmPid = msg.substring(0, msg.indexOf(" "));
                        //判断截取的PID是否为纯数字
                        if (NumberUtil.isInteger(tmPid)) {
                            command = String.format("taskkill /F /PID %s", tmPid);
                            exec(command, true);
                        }
                    }
                }
            }
        } else {
            command = String.format("cmd /k %s && cd %s/bin && start startup.bat", dcPath, tomcatPath);
            exec(command, true);
        }

        // 查询操作结果并返回
        return getStatus(tomcatInfoModel, cmd);
    }
}
