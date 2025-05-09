/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.plugin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FastByteArrayOutputStream;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.plugins.PluginConfig;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.StorageServiceFactory;
import org.h2.store.FileLister;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Recover;
import org.h2.tools.RunScript;
import org.h2.tools.Shell;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/18
 */
@PluginConfig(name = "db-h2")
@Slf4j
public class DefaultDbH2PluginImpl implements IDefaultPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        String method = StrUtil.toString(main);
        if (StrUtil.equals("backupSql", method)) {
            String url = (String) parameter.get("url");
            String user = (String) parameter.get("user");
            String password = (String) parameter.get("pass");
            String backupSqlPath = (String) parameter.get("backupSqlPath");
            List<String> tableNameList = (List<String>) parameter.get("tableNameList");
            this.backupSql(url, user, password, backupSqlPath, tableNameList);
        } else if (StrUtil.equals("restoreBackupSql", method)) {
            String backupSqlPath = (String) parameter.get("backupSqlPath");
            DataSource dataSource = (DataSource) parameter.get("dataSource");
            if (dataSource == null) {
                // 加载数据源
                dataSource = StorageServiceFactory.getInstance().get().getDsFactory().getDataSource();
            }
            this.restoreBackupSql(backupSqlPath, dataSource);
        } else if (StrUtil.equals("recoverToSql", method)) {
            File dbPath = (File) parameter.get("dbPath");
            String dbName = (String) parameter.get("dbName");
            File recoverBackup = (File) parameter.get("recoverBackup");
            return this.recover(dbPath, dbName, recoverBackup);
        } else if (StrUtil.equals("deleteDbFiles", method)) {
            File dbPath = (File) parameter.get("dbPath");
            String dbName = (String) parameter.get("dbName");
            File backupPath = (File) parameter.get("backupPath");
            this.deleteDbFiles(dbPath, dbName, backupPath);
        } else if (StrUtil.equals("hasDbFiles", method)) {
            File dbPath = (File) parameter.get("dbPath");
            String dbName = (String) parameter.get("dbName");
            return this.hasDbFiles(dbPath, dbName);
        } else {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_type.7495"));
        }
        return "done";
    }

    /**
     * 恢复
     *
     * @param dbPath        数据库路径
     * @param dbName        数据库名
     * @param recoverBackup 恢复到哪个路径
     * @return 返回恢复到 sql 文件
     * @throws SQLException sql
     */
    private File recover(File dbPath, String dbName, File recoverBackup) throws SQLException {
        String dbLocalPath = FileUtil.getAbsolutePath(dbPath);
        ArrayList<String> list = FileLister.getDatabaseFiles(dbLocalPath, dbName, true);
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        FileUtil.mkdir(recoverBackup);
        // 备份数据
        for (String s : list) {
            FileUtil.move(FileUtil.file(s), recoverBackup, true);
        }
        String absolutePath = FileUtil.getAbsolutePath(recoverBackup);
        log.info("h2 db recover backup path,{}", absolutePath);
        // 恢复数据
        Recover recover = new Recover();
        recover.runTool("-dir", absolutePath, "-db", dbName);
        return FileUtil.file(recoverBackup, dbName + ".h2.sql");
    }

    /**
     * 是否存在数据库文件
     *
     * @param dbPath 数据库路径
     * @param dbName 数据库名
     */
    private boolean hasDbFiles(File dbPath, String dbName) {
        String dbLocalPath = FileUtil.getAbsolutePath(dbPath);
        ArrayList<String> list = FileLister.getDatabaseFiles(dbLocalPath, dbName, true);
        return CollUtil.isNotEmpty(list);
    }

    /**
     * 删除数据
     *
     * @param dbPath 数据库路径
     * @param dbName 数据库名
     * @throws SQLException sql
     */
    private void deleteDbFiles(File dbPath, String dbName, File backupPath) throws SQLException {
        String dbLocalPath = FileUtil.getAbsolutePath(dbPath);
        ArrayList<String> list = FileLister.getDatabaseFiles(dbLocalPath, dbName, true);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        if (backupPath != null) {
            FileUtil.mkdir(backupPath);
            // 备份数据
            for (String s : list) {
                FileUtil.move(FileUtil.file(s), backupPath, true);
            }
        }
        // 删除数据
        DeleteDbFiles deleteDbFiles = new DeleteDbFiles();
        deleteDbFiles.runTool("-dir", dbLocalPath, "-db", dbName);
    }

    /**
     * 备份 SQL
     *
     * @param url           jdbc url
     * @param user          user
     * @param password      password
     * @param backupSqlPath backup SQL file path, absolute path
     * @param tableNameList backup table name list, if need backup all table, use null
     */
    private void backupSql(String url, String user, String password,
                           String backupSqlPath, List<String> tableNameList) throws SQLException {
        // 备份 SQL
        String sql = StrUtil.format("SCRIPT DROP to '{}'", backupSqlPath);
        // 判断是否部分部分表
        if (!CollectionUtils.isEmpty(tableNameList)) {
            String tableNames = StrUtil.join(StrUtil.COMMA, tableNameList.toArray());
            sql = StrUtil.format("{} TABLE {}", sql, tableNames);
        }
        log.debug("backup SQL is: {}", sql);
        // 执行 SQL 备份脚本
        Shell shell = new Shell();

        /*
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
            "-password", password,
            "-driver", "org.h2.Driver",
            "-sql", sql
        };
        try (FastByteArrayOutputStream arrayOutputStream = new FastByteArrayOutputStream()) {
            try (PrintStream printStream = new PrintStream(arrayOutputStream)) {
                shell.setOut(printStream);
                shell.runTool(params);
            }
        }
    }

    /**
     * 还原备份 SQL
     *
     * @param backupSqlPath backup SQL file path, absolute path
     * @throws SQLException SQLException
     * @throws IOException  FileNotFoundException
     */
    private void restoreBackupSql(String backupSqlPath, DataSource dataSource) throws SQLException, IOException {
        Assert.notNull(dataSource, "Restore Backup sql error...H2 DataSource not null");
        try (Connection connection = dataSource.getConnection()) {
            // 读取数据库备份文件，执行还原
            File backupSqlFile = FileUtil.file(backupSqlPath);
            try (FileReader fileReader = new FileReader(backupSqlFile)) {
                RunScript.execute(connection, fileReader);
            }
        }
    }
}
