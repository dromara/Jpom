package cn.keepbx.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ConfigException;
import cn.keepbx.jpom.system.WebAopLog;

import java.io.File;
import java.net.URL;

/**
 * 检查运行命令
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
@PreLoadClass
public class CheckRunCommand {

    /**
     * 检查命令文件
     */
    @PreLoadMethod
    private static void checkSh() {
        try {
            ConfigBean.getInstance().getRunCommandPath();
        } catch (ConfigException e) {
            DefaultSystemLog.LOG().info("创建默认文件：" + e.getPath());
            addCommandFile(ConfigBean.RUN_SH, e.getPath());
        }
        try {
            ConfigBean.getInstance().getRamCommandPath();
        } catch (ConfigException e) {
            DefaultSystemLog.LOG().info("创建默认文件：" + e.getPath());
            addCommandFile(ConfigBean.RAM_SH, e.getPath());
        }
        try {
            ConfigBean.getInstance().getCpuCommandPath();
        } catch (ConfigException e) {
            DefaultSystemLog.LOG().info("创建默认文件：" + e.getPath());
            addCommandFile(ConfigBean.CPU_SH, e.getPath());
        }
    }

    /**
     * 检查运行数据
     */
    @PreLoadMethod
    private static void checkData() {
        File file = new File(ConfigBean.getInstance().getDataPath(), ConfigBean.USER);
        if (!file.exists()) {
            DefaultSystemLog.LOG().info("创建默认文件：" + file.getPath());
            addDataFile(ConfigBean.USER, file.getPath());
        }

        file = new File(ConfigBean.getInstance().getDataPath(), ConfigBean.PROJECT);
        if (!file.exists()) {
            DefaultSystemLog.LOG().info("创建默认文件：" + file.getPath());
            addDataFile(ConfigBean.PROJECT, file.getPath());
        }
        WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
        String logPath = SpringUtil.getEnvironment().getProperty("jpom.log");
        DefaultSystemLog.LOG().info("日志存储路径：" + webAopLog.getPropertyValue() + "   -》  " + logPath);
    }

    /**
     * 修护检查
     */
    public static void repairData() {
        checkData();
    }

    private static void addDataFile(String name, String file) {
        URL url = ResourceUtil.getResource("bin/data/" + name);
        String content = FileUtil.readString(url, CharsetUtil.UTF_8);
        FileUtil.writeString(content, file, CharsetUtil.UTF_8);
    }

    private static void addCommandFile(String command, String file) {
        URL url = ResourceUtil.getResource("bin/command" + command);
        String content = FileUtil.readString(url, CharsetUtil.UTF_8);
        FileUtil.writeString(content, file, CharsetUtil.UTF_8);
        // 添加文件权限
        CommandService commandService = SpringUtil.getBean(CommandService.class);
        String runCommand = "chmod 755 " + file;
        commandService.execCommand(runCommand);
    }
}
