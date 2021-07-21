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
 * tomcat的linux管理命令
 *
 * @author LF
 */
public class LinuxTomcatCommander extends AbstractTomcatCommander {

    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        String tomcatPath = tomcatInfoModel.pathAndCheck();
        if (StrUtil.isBlank(tomcatPath)) {
            return "tomcat path blank";
        }
        String command = null;
        if (cmd.equals("stop")) {
            String setPidCmd = CommandUtil.execSystemCommand("jps -mv");
            List<String> list = StrSplitter.splitTrim(setPidCmd, StrUtil.LF, true);
            for (String item : list) {
                //路径格式转换
                String msg = FileUtil.normalize(item + "/");
                //判断集合中元素是否包含指定Tomcat路径
                boolean w = msg.contains(tomcatInfoModel.getPath());
                if (w) {
                    //截取TomcatPid
                    if (msg.indexOf(" ") > 1) {
                        String tmPid = msg.substring(0, msg.indexOf(" "));
                        //判断截取的PID是否为纯数字
                        if (NumberUtil.isInteger(tmPid)) {
                            command = String.format("kill -9 %s", tmPid);
                            exec(command, false);
                        }
                    }
                }
            }
        } else {
            command = String.format("/bin/sh -c %s/bin/startup.sh", tomcatPath);
            //
            exec(command, false);
        }
        if (StrUtil.isBlank(tomcatPath)) {
            return "tomcat path blank";
        }
        // 查询操作结果并返回
        return getStatus(tomcatInfoModel, cmd);
    }
}
