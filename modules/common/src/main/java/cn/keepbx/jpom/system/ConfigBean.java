package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.model.system.JpomManifest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 配置项
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Configuration
public class ConfigBean {
    public static final String JPOM_SERVER_SYSTEM_USER_ROLE = "Jpom-Server-SystemUserRole";

    public static final String JPOM_SERVER_USER_NAME = "Jpom-Server-UserName";

    public static final String JPOM_AGENT_AUTHORIZE = "Jpom-Agent-Authorize";

    private static final String DATA = "data";

    public static final int AUTHORIZE_ERROR = 900;

    /**
     * Jpom 程序运行的 application 标识
     */
    @Value("${jpom.applicationTag:}")
    public String applicationTag;

    private static ConfigBean configBean;

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
        return new File(getDataPath(), BaseJpomApplication.getAppType().name() + ".pid." + JpomManifest.getInstance().getPid());
    }

    /**
     * 获取当前Jpom 运行信息文件
     *
     * @return file
     */
    public File getJpomInfo() {
        return new File(getDataPath(), "jpom." + BaseJpomApplication.getAppType().name() + ".info");
    }
}
