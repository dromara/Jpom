package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 配置信息静态变量类
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
@Configuration
public class ConfigBean {
    private static final String DATA = "data";
    private static ConfigBean configBean;
    /**
     * 用户数据文件
     */
    public static final String USER = "user.json";
    /**
     * 项目数据文件
     */
    public static final String PROJECT = "project.json";
    /**
     * 白名单文件
     */
    public static final String WHITELIST_DIRECTORY = "whitelistDirectory.json";
    /**
     * 阿里oss 文件
     */
    public static final String ALI_OSS = "aliOss.json";
    /**
     * 证书文件
     */
    public static final String CERT = "cert.json";
    /**
     * 项目回收文件
     */
    public static final String PROJECT_RECOVER = "project_recover.json";
    /**
     * Jpom 运行的进程信息 文件
     */
    private static final String PID = "pid.info";
    /**
     * Jpom 程序运行的 application 标识
     */
    @Value("${jpom.applicationTag:}")
    public String applicationTag;

    /**
     * 单利模式
     *
     * @return config
     */
    public static ConfigBean getInstance() {
        if (configBean == null) {
            configBean = SpringUtil.getBean(ConfigBean.class);
        }
        return configBean;
    }

    /**
     * 获取项目运行数据存储文件夹路径
     *
     * @return 文件夹路径
     */
    public String getDataPath() {
        String dataPath = FileUtil.normalize(ExtConfigBean.getInstance().getPath() + "/" + DATA);
        FileUtil.mkdir(dataPath);
        return dataPath;
    }

    /**
     * 获取pid文件
     *
     * @return file
     */
    public File getPidFile() {
        return new File(getDataPath(), PID);
    }

    /**
     * 获取当前登录用户的临时文件存储路径，如果没有登录则抛出异常
     *
     * @return 文件夹
     */
    public String getTempPathName() {
        File file = getTempPath();
        return FileUtil.normalize(file.getPath());
    }

    /**
     * 获取当前登录用户的临时文件存储路径，如果没有登录则抛出异常
     *
     * @return file
     */
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
}
