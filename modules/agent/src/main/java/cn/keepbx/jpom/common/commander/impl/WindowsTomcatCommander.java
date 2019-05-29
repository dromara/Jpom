package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.thread.GlobalThreadPool;
import cn.keepbx.jpom.common.commander.AbstractTomcatCommander;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.util.CommandUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class WindowsTomcatCommander extends AbstractTomcatCommander {
//    private static final String startCMD = "start.bat";
//    private static final String stopCMD = "shutdown.bat";

    @Override
    public String execCmd(TomcatInfoModel tomcatInfoModel, String cmd) {

        // 拼接命令
//        String command = String.format("javaw -Djava.util.logging.config.file=\"%sconf/logging.properties\" " +
//                "-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager  " +
//                "-Djdk.tls.ephemeralDHKeySize=2048" +
//                "-Djava.protocol.handler.pkgs=org.apache.catalina.webresources   " +
//                "-Dignore.endorsed.dirs=\"%s\" " +
//                "-classpath \"%sbin/bootstrap.jar;%sbin/tomcat-juli.jar\" " +
//                "-Dcatalina.base=\"%s\" " +
//                "-Dcatalina.home=\"%s\" " +
//                "-Djava.io.tmpdir=\"%stemp/\" " +
//                "org.apache.catalina.startup.Bootstrap  start", tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), tomcatInfoModel.getPath(), tomcatInfoModel.getPath());
//        String command = "java -Djava.util.logging.config.file=\"D:\\apache-tomcat-8.5.40\\conf\\logging.properties\" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager  \"-Djdk.tls.ephemeralDHKeySize=2048\" -Djava.protocol.handler.pkgs=org.apache.catalina.webresources   -Dignore.endorsed.dirs=\"\" -classpath \"D:\\apache-tomcat-8.5.40\\bin\\bootstrap.jar;D:\\apache-tomcat-8.5.40\\bin\\tomcat-juli.jar\" -Dcatalina.base=\"D:\\apache-tomcat-8.5.40\" -Dcatalina.home=\"D:\\apache-tomcat-8.5.40\" -Djava.io.tmpdir=\"D:\\apache-tomcat-8.5.40\\temp\" org.apache.catalina.startup.Bootstrap  start";
        // 执行命令
//        GlobalThreadPool.execute(() -> CommandUtil.execSystemCommand(command));
//        Runtime r = Runtime.getRuntime();
//        String command = String.format("cmd /c %s/bin/startup.bat", tomcatInfoModel.getPath());

        String command = "java -Djava.util.logging.config.file=\"D:\\apache-tomcat-8.5.40\\conf\\logging.properties\" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager  \"-Djdk.tls.ephemeralDHKeySize=2048\" -Djava.protocol.handler.pkgs=org.apache.catalina.webresources   -Dignore.endorsed.dirs=\"\" -classpath \"D:\\apache-tomcat-8.5.40\\bin\\bootstrap.jar;D:\\apache-tomcat-8.5.40\\bin\\tomcat-juli.jar\" -Dcatalina.base=\"D:\\apache-tomcat-8.5.40\" -Dcatalina.home=\"D:\\apache-tomcat-8.5.40\" -Djava.io.tmpdir=\"D:\\apache-tomcat-8.5.40\\temp\" org.apache.catalina.startup.Bootstrap  start ";
        System.out.println(command);

        try {
            Runtime.getRuntime().exec(command, null, new File(tomcatInfoModel.getPath()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    @Override
    public String status() {
        return null;
    }
}
