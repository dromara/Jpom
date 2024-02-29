/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.h2db;

import cn.hutool.core.io.FileUtil;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.sql.SqlLog;
import cn.hutool.setting.Setting;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.ApplicationStartTest;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.system.ExtConfigBean;
import org.h2.tools.RunScript;
import org.h2.tools.Shell;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Hotstrip
 * @since 2021-11-01
 * 测试 H2 数据库工具类
 */
@Slf4j
public class H2ToolTest extends ApplicationStartTest {


    /**
     * 备份 SQL 文件
     * 如果抛出了异常或者指定的备份文件不存在就表示备份不成功
     *
     * @throws SQLException
     */
    @Test
    public void testH2ShellForBackupSQL() throws SQLException {
        // 数据源参数
        String url = StorageServiceFactory.get().dbUrl();

        String user = dbExtConfig.userName();
        String pass = dbExtConfig.userPwd();

        // 备份 SQL 的目录
        File file = FileUtil.file(ExtConfigBean.getPath(), "db", JpomApplication.getAppType().name());
        String path = FileUtil.getAbsolutePath(file);

        log.info("url: {}", url);
        log.info("user: {}", user);
        log.info("pass: {}", pass);
        log.info("backup sql path: {}", path);

        // 加载数据源
        DataSource dataSource = DSFactory.get();
        if (null == dataSource) {
            dataSource = initDataSource(url, user, pass);
        }
        Connection connection = dataSource.getConnection();

        // 执行 SQL 备份脚本
        Shell shell = new Shell();

        /**
         * url 表示 h2 数据库的 jdbc url
         * user 表示登录的用户名
         * password 表示登录密码
         * driver 是 jdbc 驱动
         * sql 是备份的 sql 语句
         * - 案例：script drop to ${fileName1} table ${tableName1},${tableName2}...
         * - script drop to 表示备份数据库，drop 表示建表之前会先删除表
         * - ${fileName1} 表示备份之后的文件名
         * - table 表示需要备份的表名称，后面跟多个表名，用英文逗号分割
         */
        String[] params = new String[]{
            "-url", url,
            "-user", user,
            "-password", pass,
            "-driver", "org.h2.Driver",
            "-sql", "script DROP to '" + path + "/backup.sql' table BUILD_INFO,USEROPERATELOGV1"
        };
        shell.runTool(connection, params);
    }

    /**
     * 还原 SQL 文件
     */
    @Test
    public void testH2RunScriptWithBackupSQL() throws SQLException, FileNotFoundException {
        // 备份 SQL
        testH2ShellForBackupSQL();

        // 恢复之前先删除数据库数据，以免冲突
        testH2DropAllObjects();

        // 备份 SQL 的目录
        File file = FileUtil.file(ExtConfigBean.getPath(), "db", JpomApplication.getAppType().name());
        String path = FileUtil.getAbsolutePath(file) + "/backup.sql";

        FileReader fileReader = new FileReader(path);

        // 加载数据源
        DataSource dataSource = DSFactory.get();
        Connection connection = dataSource.getConnection();

        RunScript.execute(connection, fileReader);
    }

    /**
     * H2 drop all objects
     * 删除 H2 数据库所有数据
     *
     * @throws SQLException
     */
    @Test
    public void testH2DropAllObjects() throws SQLException {
        // 数据源参数
        String url = StorageServiceFactory.get().dbUrl();

        String user = dbExtConfig.userName();
        String pass = dbExtConfig.userPwd();

        // 加载数据源
        DataSource dataSource = DSFactory.get();
        Connection connection = dataSource.getConnection();

        // 执行 SQL 备份脚本
        Shell shell = new Shell();

        String[] params = new String[]{
            "-url", url,
            "-user", user,
            "-password", pass,
            "-driver", "org.h2.Driver",
            "-sql", "drop all objects"
        };
        shell.runTool(connection, params);
    }

    /**
     * 初始化数据源
     *
     * @return
     */
    private DataSource initDataSource(String url, String user, String pass) {
        // 数据源配置
        Setting setting = new Setting();
        setting.set("url", url);
        setting.set("user", user);
        setting.set("pass", pass);
        // 配置连接池大小
        setting.set("maxActive", "50");
        setting.set("initialSize", "1");
        setting.set("maxWait", "10");
        setting.set("minIdle", "1");
        // show sql
        setting.set(SqlLog.KEY_SHOW_SQL, "true");
        setting.set(SqlLog.KEY_SQL_LEVEL, "DEBUG");
        setting.set(SqlLog.KEY_SHOW_PARAMS, "true");
        log.info("start load h2 db");
        // 创建连接
        DSFactory dsFactory = DSFactory.create(setting);
        return dsFactory.getDataSource();
    }

}
