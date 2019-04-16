package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseController;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 配置信息静态变量类
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
@Configuration
public class ServerConfigBean {

    private static ServerConfigBean serverConfigBean;
    /**
     * 用户数据文件
     */
    public static final String USER = "user.json";
    /**
     * 项目数据文件
     */
    public static final String PROJECT = "project.json";
    /**
     * 节点数据文件
     */
    public static final String NODE = "node.json";

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
     * 单利模式
     *
     * @return config
     */
    public static ServerConfigBean getInstance() {
        if (serverConfigBean == null) {
            serverConfigBean = SpringUtil.getBean(ServerConfigBean.class);
        }
        return serverConfigBean;
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
        File file = new File(ConfigBean.getInstance().getDataPath());
        String userName = BaseController.getUserName();
        if (StrUtil.isEmpty(userName)) {
            throw new JpomRuntimeException("没有登录");
        }
        file = new File(file.getPath() + "/temp/", userName);
        FileUtil.mkdir(file);
        return file;
    }
}
