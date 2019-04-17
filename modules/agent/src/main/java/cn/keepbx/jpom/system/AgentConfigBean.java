package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.controller.base.AbstractController;
import cn.keepbx.jpom.common.BaseAgentController;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

/**
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

    /**
     * 项目回收文件
     */
    public static final String PROJECT_RECOVER = "project_recover.json";

    /**
     * 阿里oss 文件
     */
    public static final String ALI_OSS = "aliOss.json";

    /**
     * 证书文件
     */
    public static final String CERT = "cert.json";


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
        File file = new File(ConfigBean.getInstance().getDataPath());
        HttpServletRequest request = AbstractController.getRequestAttributes().getRequest();
        String userName = BaseAgentController.getUserName(request);
        if (StrUtil.isEmpty(userName)) {
            throw new JpomRuntimeException("没有登录");
        }
        file = new File(file.getPath() + "/temp/", userName);
        FileUtil.mkdir(file);
        return file;
    }
}
