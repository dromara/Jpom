package cn.keepbx.jpom.system.init;

import cn.hutool.db.DbUtil;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.setting.Setting;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.system.db.DbConfig;

import javax.sql.DataSource;
import java.sql.Connection;


/**
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
        //
        if (JpomManifest.getInstance().isDebug()) {
            setting.set("showSql", "true");
            setting.set("showParams", "true");
        }

        try {
            DSFactory dsFactory = DSFactory.create(setting);
            DataSource dataSource = dsFactory.getDataSource();
            Connection connection = dataSource.getConnection();
            DbUtil.close(connection);
            DSFactory.setCurrentDSFactory(dsFactory);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("初始化数据失败", e);
            System.exit(0);
        }
    }
}
