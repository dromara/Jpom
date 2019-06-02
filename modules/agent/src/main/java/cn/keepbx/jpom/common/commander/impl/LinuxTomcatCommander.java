package cn.keepbx.jpom.common.commander.impl;

import cn.keepbx.jpom.common.commander.AbstractTomcatCommander;
import cn.keepbx.jpom.model.data.TomcatInfoModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * tomcat的linux管理命令
 * @author LF
 */
public class LinuxTomcatCommander extends AbstractTomcatCommander {

    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {


        // 拼接命令
        String command = String.format("java -Djava.util.logging.config.file=%sconf/logging.properties " +
                        "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager " +
                        "-Djdk.tls.ephemeralDHKeySize=2048 " +
                        "-Djava.protocol.handler.pkgs=org.apache.catalina.webresources " +
                        "-Dignore.endorsed.dirs=%s " +
                        "-classpath %sbin/bootstrap.jar:%sbin/tomcat-juli.jar " +
                        "-Dcatalina.base=%s " +
                        "-Dcatalina.home=%s " +
                        "-Djava.io.tmpdir=%stemp/ " +
                        "org.apache.catalina.startup.Bootstrap %s", tomcatInfoModel.getPath(), tomcatInfoModel.getPath(),
                tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), tomcatInfoModel.getPath(),
                tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), cmd);
        try {
            // 执行命令
            Process process = Runtime.getRuntime().exec(command);
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
            process.waitFor(5, TimeUnit.SECONDS);
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // 查询操作结果并返回
        return getStatus(tomcatInfoModel, cmd);
    }
}
