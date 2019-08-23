package io.jpom.system.db;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.JpomApplication;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.ServerExtConfigBean;

import java.io.File;
import java.sql.SQLException;

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
        File file = FileUtil.file(ExtConfigBean.getInstance().getAbsolutePath(), DB, JpomApplication.getAppType().name());
        String path = FileUtil.getAbsolutePath(file);
        return StrUtil.format("jdbc:h2:{}", path);
    }

    /**
     * 清除超限制数量的数据
     *
     * @param tableName 表名
     * @param timeClo   时间字段名
     */
    public static void autoClear(String tableName, String timeClo) {
        if (ServerExtConfigBean.getInstance().getH2DbLogStorageCount() <= 0) {
            return;
        }
        ThreadUtil.execute(() -> {
            Entity entity = Entity.create(tableName);
            Page page = new Page(ServerExtConfigBean.getInstance().getH2DbLogStorageCount(), 1);
            page.addOrder(new Order(timeClo, Direction.DESC));
            PageResult<Entity> pageResult;
            try {
                pageResult = Db.use().setWrapper((Character) null).page(entity, page);
                if (pageResult.isEmpty()) {
                    return;
                }
                Entity entity1 = pageResult.get(0);
                long time = Convert.toLong(entity1.get(timeClo.toUpperCase()), 0L);
                if (time <= 0) {
                    return;
                }
                entity.set(timeClo, "< " + time);
                int count = Db.use().setWrapper((Character) null).del(entity);
                DefaultSystemLog.LOG().info("{} 清理了 {}条数据", tableName, count);
            } catch (SQLException e) {
                DefaultSystemLog.ERROR().error("数据库查询异常", e);
            }
        });
    }
}
