package io.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ClassUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.system.ConfigBean;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.StringUtil;

import java.io.File;
import java.util.Arrays;

/**
 * 数据目录权限检查
 *
 * @author jiangzeyin
 * @date 2019/3/26
 */
@PreLoadClass
public class CheckPath {
    /**
     * 待检查的类
     */
    private static final String[] CLASS_NAME = new String[]{"sun.jvmstat.monitor.MonitorException", "com.sun.tools.attach.VirtualMachine"};


    @PreLoadMethod(1)
    private static void checkToolsJar() {
        try {
            for (String item : CLASS_NAME) {
                ClassUtil.loadClass(item, false);
            }
        } catch (Exception e) {
            File file = StringUtil.getToolsJar();
            if (file.exists() && file.isFile()) {
                DefaultSystemLog.getLog().error("Jpom未能正常加载tools.jar,请检查当前系统环境变量是否配置：JAVA_HOME，或者检查Jpom管理命令是否正确", e);
            } else {
                DefaultSystemLog.getLog().error("当前JDK中没有找到tools.jar,请检查当前JDK是否安装完整，文件完整路径是：" + file.getAbsolutePath(), e);
            }
            System.exit(-1);
        }
    }

    /**
     * 判断是否重复运行
     */
    @PreLoadMethod(2)
    private static void checkDuplicateRun() {
        CheckDuplicateRun.check();
    }

    @PreLoadMethod(3)
    private static void reqXssLog() {
        if (!ExtConfigBean.getInstance().isConsoleLogReqXss()) {
            // 不在控制台记录请求日志信息
            DefaultSystemLog.setLogCallback((type, log) -> {
                //
                if (type == DefaultSystemLog.LogType.REQUEST_ERROR) {
                    DefaultSystemLog.getLog().info(Arrays.toString(log));
                }
            });
        }
    }

    @PreLoadMethod(4)
    private static void clearTemp() {
        File file = ConfigBean.getInstance().getTempPath();
        FileUtil.clean(file);
    }
}
