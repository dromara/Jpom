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
     * 节点数据文件
     */
    public static final String NODE = "node.json";


    /**
     * 证书文件
     */
    public static final String CERT = "cert.json";


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
//        File file = getTempPath();
        return null;
//        return FileUtil.normalize(file.getPath());
    }


}
