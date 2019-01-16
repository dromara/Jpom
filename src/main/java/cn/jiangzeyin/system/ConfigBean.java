package cn.jiangzeyin.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.controller.BaseController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * @author jiangzeyin
 * @date 2019/1/16
 */
@Configuration
public class ConfigBean {
    private static final String COMMAND = "command";
    private static final String DATA = "data";
    private static ConfigBean configBean;

    public static final String RUN_SH = "/run_boot.sh";
    public static final String CPU_SH = "/java_cpu.sh";
    public static final String RAM_SH = "/java_ram.sh";

    public static final String USER = "user.json";
    public static final String PROJECT = "project.json";

    public static ConfigBean getInstance() {
        if (configBean == null) {
            synchronized (ConfigBean.class) {
                if (configBean == null) {
                    configBean = SpringUtil.getBean(ConfigBean.class);
                }
            }
        }
        return configBean;
    }

    @Value("${jpom.path}")
    private String path;

    private String getPath() {
        if (StrUtil.isEmpty(path)) {
            throw new RuntimeException("请配运行路径属性【jpom.path】");
        }
        return path;
    }

    private String getCommandPath() {
        String commandPath = FileUtil.normalize(getPath() + "/" + COMMAND);
        FileUtil.mkdir(commandPath);
        return commandPath;
    }

    public String getDataPath() {
        String dataPath = FileUtil.normalize(getPath() + "/" + DATA);
        FileUtil.mkdir(dataPath);
        return dataPath;
    }

    public String getTempPathName() {
        File file = getTempPath();
        return FileUtil.normalize(file.getPath());
    }

    public File getTempPath() {
        File file = new File(getDataPath());
        String userName = BaseController.getUserName();
        if (StrUtil.isEmpty(userName)) {
            throw new RuntimeException("没有登录");
        }
        file = new File(file.getPath() + "/temp/", userName);
        FileUtil.mkdir(file);
        return file;
    }

    private String getCommandPath(String item) throws ConfigException {
        String command = getCommandPath();
        String runSh = FileUtil.normalize(command + item);
        if (!FileUtil.exist(runSh)) {
            throw new ConfigException(item + " 文件不存在:" + runSh, runSh);
        }
        return runSh;
    }


    public String getRunCommandPath() throws ConfigException {
        return getCommandPath(RUN_SH);
    }


    public String getCpuCommandPath() throws ConfigException {
        return getCommandPath(CPU_SH);
    }


    public String getRamCommandPath() throws ConfigException {
        return getCommandPath(RAM_SH);
    }
}
