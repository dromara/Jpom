package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.UserModel;
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
    public File getTempPath() {
        File file = new File(ConfigBean.getInstance().getDataPath());
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null) {
            throw new JpomRuntimeException("没有登录");
        }
        file = new File(file.getPath() + "/temp/", userModel.getId());
        FileUtil.mkdir(file);
        return file;
    }
}
