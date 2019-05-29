package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.http.HttpRequest;
import cn.keepbx.jpom.common.commander.AbstractTomcatCommander;
import cn.keepbx.jpom.model.data.TomcatInfoModel;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WindowsTomcatCommander extends AbstractTomcatCommander {
    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {
        String strReturn = "start".equals(cmd) ? "stopped" : "started";

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
                "org.apache.catalina.startup.Bootstrap %s", tomcatInfoModel.getPath(), tomcatInfoModel.getPath(),
                tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), tomcatInfoModel.getPath(),
                tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), cmd);
        try {
            Process process = Runtime.getRuntime().exec(command, null, new File(tomcatInfoModel.getPath()));

            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
            process.waitFor(5, TimeUnit.SECONDS);
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // 检查tomcat是否已经启动或停止
        int i = 0;
        while (i < 10) {
            int result = 0;
            String url = String.format("http://127.0.0.1:%d/", tomcatInfoModel.getPort());
            HttpRequest httpRequest = new HttpRequest(url);
            httpRequest.setConnectionTimeout(3000); // 设置超时时间为3秒
            try {
                httpRequest.execute();
                result = 1;
            } catch (Exception ignored) {}

            i++;
            if ("start".equals(cmd) && result == 1) {
                strReturn = "started";
                break;
            }
            if ("stop".equals(cmd) && result == 0) {
                strReturn = "stopped";
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }

        return strReturn;
    }
}
