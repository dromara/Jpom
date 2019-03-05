package cn.keepbx.jpom.system;

import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationHome;
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

    private static File file;

    /**
     * 动态获取外部配置文件的 file
     *
     * @return File
     */
    public static File getFile() {
        if (file != null) {
            return file;
        }
        ApplicationHome home = new ApplicationHome(ExtConfigBean.class);
        String path = (home.getSource() == null ? "" : home.getSource().getAbsolutePath());
        File file = new File(path);
        if (file.isFile()) {
            file = file.getParentFile().getParentFile();
        } else {
            file = new File(file, "bin");
        }
        ExtConfigBean.file = new File(file, FILE_NAME);
        return ExtConfigBean.file;
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
