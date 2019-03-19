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

    public static final String USER = "user.json";
    public static final String PROJECT = "project.json";
    public static final String WHITELIST_DIRECTORY = "whitelistDirectory.json";
    public static final String ALI_OSS = "aliOss.json";
    public static final String CERT = "cert.json";


    public static final String JPOM_PATH = "jpom.path";


    public static ConfigBean getInstance() {
        if (configBean == null) {
            configBean = SpringUtil.getBean(ConfigBean.class);
        }
        return configBean;
    }

    /**
     * 标记是否为安全模式
     */
    @Value("${jpom.safeMode:false}")
    public boolean safeMode;

    /**
     * 项目运行存储路径
     */
    @Value("${" + JPOM_PATH + "}")
    private String path;

    public String getPath() {
        if (StrUtil.isEmpty(path)) {
            throw new RuntimeException("请配运行路径属性【jpom.path】");
        }
        return path;
    }

    /**
     * 获取项目运行数据存储文件夹路径
     *
     * @return 文件夹路径
     */
    public String getDataPath() {
        String dataPath = FileUtil.normalize(getPath() + "/" + DATA);
        FileUtil.mkdir(dataPath);
        return dataPath;
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
