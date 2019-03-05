package cn.keepbx.jpom.system;

import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/04
 */
@Configuration
public class ExtConfigBean {

    public static final String FILE_NAME = "extConfig.yml";

    public static final File FILE;

    static {
        File directory = new File("");
        FILE = new File(directory.getAbsolutePath(), ExtConfigBean.FILE_NAME);
    }

    /**
     * 白名单路径是否判断包含关系
     */
    @Value("${whitelistDirectory.checkStartsWith:true}")
    public boolean whitelistDirectoryCheckStartsWith = true;


    public static ExtConfigBean getInstance() {
        return SpringUtil.getBean(ExtConfigBean.class);
    }
}
