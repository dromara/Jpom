package io.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.UserModel;
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
    /**
     * 用户数据文件
     */
    public static final String USER = "user.json";

    /**
     * 节点数据文件
     */
    public static final String NODE = "node.json";

    /**
     * 分发数据文件
     */
    public static final String OUTGIVING = "outgiving.json";

    /**
     * 白名单数据
     */
    public static final String OUTGIVING_WHITELIST = "outgiving_whitelist.json";

    /**
     * 分发包存储路径
     */
    public static final String OUTGIVING_FILE = "outgiving";

    /**
     * 项目监控文件
     */
    public static final String MONITOR_FILE = "monitor.json";

    /**
     * 监控用户操作文件
     */
    public static final String MONITOR_USER_OPT_FILE = "monitor_user_opt.json";

    /**
     * 邮箱配置
     */
    public static final String MAIL_CONFIG = "mail_config.json";

    /**
     * 构建数据
     */
    public static final String BUILD = "build.json";

    /**
     * 第一次服务端安装信息
     */
    public static final String INSTALL = "INSTALL.json";

    /**
     * ssh信息
     */
    public static final String SSH_LIST = "ssh_list.json";

    /**
     * 用户角色信息
     */
    public static final String ROLE = "user_role.json";

    /**
     * ip配置
     */
    public static final String IP_CONFIG = "ip_config.json";

    /**
     * token自动续签状态码
     */
    public static final int RENEWAL_AUTHORIZE_CODE = 801;

    /**
     * token 失效
     */
    public static final int AUTHORIZE_TIME_OUT_CODE = 800;

    private static ServerConfigBean serverConfigBean;

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
     * @return file
     */
    public File getUserTempPath() {
        File file = getTempPath();
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null) {
            throw new JpomRuntimeException("没有登录");
        }
        file = FileUtil.file(file, userModel.getId());
        FileUtil.mkdir(file);
        return file;
    }

    public File getTempPath() {
        File file = new File(ConfigBean.getInstance().getDataPath());
        file = new File(file.getPath() + "/temp/");
        FileUtil.mkdir(file);
        return file;
    }
}
