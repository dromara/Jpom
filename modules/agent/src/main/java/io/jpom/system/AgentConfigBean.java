package io.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseAgentController;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 插件端配置
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Configuration
public class AgentConfigBean {
    /**
     * 白名单文件
     */
    public static final String WHITELIST_DIRECTORY = "whitelistDirectory.json";
    /**
     * 项目数据文件
     */
    public static final String PROJECT = "project.json";

    public static final String TOMCAT = "tomcat.json";
    /**
     * 项目回收文件
     */
    public static final String PROJECT_RECOVER = "project_recover.json";
    /**
     * 证书文件
     */
    public static final String CERT = "cert.json";
    /**
     * 脚本管理数据文件
     */
    public static final String SCRIPT = "script.json";
    /**
     * 脚本模板存放路径
     */
    public static final String SCRIPT_DIRECTORY = "script";

    /**
     * Server 端的信息
     */
    public static final String SERVER_ID = "SERVER.json";

    /**
     * nginx配置信息
     */
    public static final String NGINX_CONF = "nginx_conf.json";

    /**
     * jdk列表信息
     */
    public static final String JDK_CONF = "jdk_conf.json";

    private static AgentConfigBean agentConfigBean;

    /**
     * 单利模式
     *
     * @return config
     */
    public static AgentConfigBean getInstance() {
        if (agentConfigBean == null) {
            agentConfigBean = SpringUtil.getBean(AgentConfigBean.class);
        }
        return agentConfigBean;
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
        File file = ConfigBean.getInstance().getTempPath();
        String userName = BaseAgentController.getNowUserName();
        if (StrUtil.isEmpty(userName)) {
            throw new JpomRuntimeException("没有登录");
        }
        file = new File(file, userName);
        FileUtil.mkdir(file);
        return file;
    }

    /**
     * 获取脚本模板路径
     *
     * @return file
     */
    public File getScriptPath() {
        return FileUtil.file(ConfigBean.getInstance().getDataPath(), SCRIPT_DIRECTORY);
    }
}
