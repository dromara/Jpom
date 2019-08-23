package io.jpom.system.init;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.setting.Setting;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.common.JpomManifest;
import io.jpom.system.db.DbConfig;

import java.io.InputStream;


/**
 * 初始化数据库
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@PreLoadClass
public class InitDb {

    @PreLoadMethod
    private static void init() {
        Setting setting = new Setting();
        setting.set("url", DbConfig.getInstance().getDbUrl());
        setting.set("user", "jpom");
        setting.set("pass", "jpom");
        // 调试模式显示sql 信息
        if (JpomManifest.getInstance().isDebug()) {
            setting.set("showSql", "true");
            setting.set("sqlLevel", "INFO");
            setting.set("showParams", "true");
        }
        DefaultSystemLog.LOG().info("初始化数据中....");
        try {
            // 创建连接
            DSFactory dsFactory = DSFactory.create(setting);
            InputStream inputStream = ResourceUtil.getStream("classpath:/bin/h2-db-v1.sql");
            String sql = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
            Db.use(dsFactory.getDataSource()).execute(sql);
            DSFactory.setCurrentDSFactory(dsFactory);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("初始化数据库失败", e);
            System.exit(0);
        }
    }
}
