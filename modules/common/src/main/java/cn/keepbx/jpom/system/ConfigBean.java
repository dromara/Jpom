package cn.keepbx.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.system.JpomManifest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Configuration
public class ConfigBean {
    private static final String DATA = "data";

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
        return new File(getDataPath(), "pid." + JpomManifest.getInstance().getPid());
    }

    /**
     * 获取当前Jpom 运行信息文件
     *
     * @return file
     */
    public File getJpomInfo() {
        return new File(getDataPath(), "jpom.info");
    }
}
