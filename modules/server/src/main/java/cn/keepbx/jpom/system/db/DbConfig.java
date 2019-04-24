package cn.keepbx.jpom.system.db;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.system.ExtConfigBean;

import java.io.File;

/**
 * 数据库配置
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class DbConfig {

    private static final String DB = "db";

    private static DbConfig dbConfig;

    /**
     * 单利模式
     *
     * @return config
     */
    public static DbConfig getInstance() {
        if (dbConfig == null) {
            dbConfig = new DbConfig();
        }
        return dbConfig;
    }

    /**
     * 获取数据库的jdbc 连接
     *
     * @return jdbc
     */
    public String getDbUrl() {
        File file = FileUtil.file(ExtConfigBean.getInstance().getAbsolutePath(), DB, BaseJpomApplication.getAppType().name());
        String path = FileUtil.getAbsolutePath(file);
        return StrUtil.format("jdbc:h2:{}", path);
    }
}
