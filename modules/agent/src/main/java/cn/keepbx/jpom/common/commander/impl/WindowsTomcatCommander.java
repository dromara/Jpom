package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.commander.AbstractTomcatCommander;
import cn.keepbx.jpom.model.data.TomcatInfoModel;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        if (StrUtil.isBlank(tomcatPath)) {
            return "tomcat path blank";
        }
        // 拼接命令
        String command = String.format("cmd /c java -Djava.util.logging.config.file=\"%sconf/logging.properties\" " +
                        "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager  " +
                        "-Djdk.tls.ephemeralDHKeySize=2048" +
                        "-Djava.protocol.handler.pkgs=org.apache.catalina.webresources   " +
                        "-Dignore.endorsed.dirs=\"%s\" " +
                        "-classpath \"%sbin/bootstrap.jar;%sbin/tomcat-juli.jar\" " +
                        "-Dcatalina.base=\"%s\" " +
                        "-Dcatalina.home=\"%s\" " +
                        "-Djava.io.tmpdir=\"%stemp/\" " +
                        "org.apache.catalina.startup.Bootstrap %s", tomcatPath, tomcatPath,
                tomcatPath, tomcatPath, tomcatPath,
                tomcatPath, tomcatPath, cmd);
        try {
            // 执行命令
            Process process = Runtime.getRuntime().exec(command, null, new File(tomcatPath));

            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
            process.waitFor(5, TimeUnit.SECONDS);
            process.destroy();
        } catch (IOException | InterruptedException e) {
            DefaultSystemLog.ERROR().error("tomcat执行名称失败", e);
        }

        // 查询操作结果并返回
        return getStatus(tomcatInfoModel, cmd);
    }
}
