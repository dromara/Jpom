/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.storage;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.setting.Setting;
import cn.keepbx.jpom.plugins.IPlugin;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.db.IStorageService;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.system.JpomRuntimeException;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import org.h2.jdbc.JdbcSQLNonTransientException;
import org.h2.mvstore.MVStoreException;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/1/5
 */
@Slf4j
public class H2StorageServiceImpl implements IStorageService {

    private String dbUrl;
    private DSFactory dsFactory;

    @Override
    public String dbUrl() {
        Assert.hasText(this.dbUrl, I18nMessageUtil.get("i18n.database_not_initialized.e5e7"));
        return dbUrl;
    }

    @Override
    public int getFetchSize() {
        return 100;
    }

    @Override
    public DbExtConfig.Mode mode() {
        return DbExtConfig.Mode.H2;
    }

    @Override
    public DSFactory create(DbExtConfig dbExtConfig, String url, String user, String pass) {
        Setting setting = this.createSetting(dbExtConfig, url, user, pass);
        return DSFactory.create(setting);
    }

    @Override
    public Setting createSetting(DbExtConfig dbExtConfig, String url, String user, String pass) {
        String url2 = Opt.ofBlankAble(url).orElseGet(() -> this.getDefaultDbUrl(dbExtConfig));
        String user2 = Opt.ofBlankAble(user).orElse(DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION);
        String pass2 = Opt.ofBlankAble(pass).orElse(DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION);
        Setting setting = dbExtConfig.toSetting();
        setting.set("user", user2);
        setting.set("pass", pass2);
        setting.set("url", url2);
        return setting;
    }

    @Override
    public DSFactory init(DbExtConfig dbExtConfig) {
        Assert.isNull(this.dsFactory, I18nMessageUtil.get("i18n.do_not_reinitialize_database.9bb5"));
        Setting setting = dbExtConfig.toSetting();
        //
        String dbUrl = this.getDbUrl(dbExtConfig);
        setting.set("url", dbUrl);
        // 安全检查
        dbSecurityCheck(dbExtConfig);
        this.dsFactory = DSFactory.create(setting);
        this.dbUrl = dbUrl;
        return this.dsFactory;
    }

    public DSFactory getDsFactory() {
        Assert.notNull(this.dsFactory, I18nMessageUtil.get("i18n.database_not_initialized.e5e7"));
        return dsFactory;
    }

    /**
     * 数据库是否开启 web 配置检查
     * <p>
     * <a href=http://${ip}:${port}/h2-console>http://${ip}:${port}/h2-console</a>
     *
     * @param dbExtConfig 外部配置
     * @see H2ConsoleProperties#getEnabled()
     */
    private void dbSecurityCheck(DbExtConfig dbExtConfig) {


        String property = SpringUtil.getApplicationContext().getEnvironment().getProperty("spring.h2.console.enabled");
        boolean h2ConsoleEnabled = BooleanUtil.toBoolean(property);
        if (!JpomManifest.getInstance().isDebug() && h2ConsoleEnabled
            && StrUtil.equals(dbExtConfig.userName(), DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION)
            && StrUtil.equals(dbExtConfig.userPwd(), DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION)) {
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.security_warning_h2_console.4669"));
        }
    }

    /**
     * 获取数据库的jdbc 连接
     *
     * @return jdbc
     */
    public String getDbUrl(DbExtConfig dbExtConfig) {
        String dbUrl = dbExtConfig.getUrl();
        if (StrUtil.isNotEmpty(dbUrl)) {
            return dbUrl;
        }
        return this.getDefaultDbUrl(dbExtConfig);
    }

    /**
     * 获取数据库的jdbc 连接
     *
     * @return jdbc
     */
    public String getDefaultDbUrl(DbExtConfig dbExtConfig) {
        File file = FileUtil.file(StorageServiceFactory.dbLocalPath(), this.getDbName());
        String path = FileUtil.getAbsolutePath(file);
        return StrUtil.format("jdbc:h2:{};CACHE_SIZE={};MODE=MYSQL;LOCK_TIMEOUT=10000", path, dbExtConfig.getCacheSize().toKilobytes());
    }

    private String getDbName() {
        return JpomApplication.getAppType().name();
    }

    @Override
    public JpomRuntimeException warpException(Exception e) {
        String message = e.getMessage();
        if (e instanceof MVStoreException || ExceptionUtil.isCausedBy(e, MVStoreException.class)) {
            if (StrUtil.containsIgnoreCase(message, "The write format 1 is smaller than the supported format 2")) {
                log.warn(message);
                String tip = I18nMessageUtil.get("i18n.upgrade_database_process.e604") + StrUtil.LF;
                tip += StrUtil.TAB + I18nMessageUtil.get("i18n.export_low_version_data.f1aa") + StrUtil.LF;
                tip += StrUtil.TAB + I18nMessageUtil.get("i18n.import_low_version_data_to_new_version.247b");
                return new JpomRuntimeException(I18nMessageUtil.get("i18n.incompatible_database_version.8f7b") + StrUtil.LF + tip + StrUtil.LF, -1);
            }
        }
        if (e instanceof JdbcSQLNonTransientException || ExceptionUtil.isCausedBy(e, JdbcSQLNonTransientException.class)) {
            return new JpomRuntimeException(I18nMessageUtil.get("i18n.database_corrupted.944e") + message, e);
        }
        if (e instanceof JdbcSQLNonTransientConnectionException) {
            return new JpomRuntimeException(I18nMessageUtil.get("i18n.database_exception_due_to_resources.dbf1") + message, e);
        }
        return new JpomRuntimeException(I18nMessageUtil.get("i18n.database_exception.4894"), e);
    }


    /**
     * 恢复数据库
     */
    public File recoverDb() throws Exception {
        File dbLocalPathFile = StorageServiceFactory.dbLocalPath();
        if (!FileUtil.exist(dbLocalPathFile)) {
            return null;
        }
        String dbName = this.getDbName();
        File recoverBackup = FileUtil.file(dbLocalPathFile, "recover_backup", DateTime.now().toString(DatePattern.PURE_DATETIME_FORMAT));
        //
        IPlugin plugin = PluginFactory.getPlugin("db-h2");
        Map<String, Object> map = new HashMap<>(10);
        map.put("dbName", dbName);
        map.put("dbPath", dbLocalPathFile);
        map.put("recoverBackup", recoverBackup);
        File backupSql = (File) plugin.execute("recoverToSql", map);
        // 清空记录
        StorageServiceFactory.clearExecuteSqlLog();
        // 记录恢复的 sql
        return backupSql;
    }

    @Override
    public boolean hasDbData() throws Exception {
        File dbLocalPathFile = StorageServiceFactory.dbLocalPath();
        if (!FileUtil.exist(dbLocalPathFile)) {
            return false;
        }
        IPlugin plugin = PluginFactory.getPlugin("db-h2");
        Map<String, Object> map = new HashMap<>(10);
        map.put("dbName", this.getDbName());
        map.put("dbPath", dbLocalPathFile);
        Object hasDbFiles = plugin.execute("hasDbFiles", map);
        return BooleanUtil.toBoolean(StrUtil.toStringOrNull(hasDbFiles));
    }

    /**
     * 恢复数据库
     */
    @Override
    public String deleteDbFiles() throws Exception {
        File dbLocalPathFile = StorageServiceFactory.dbLocalPath();
        if (!FileUtil.exist(dbLocalPathFile)) {
            return null;
        }
        File deleteBackup = FileUtil.file(dbLocalPathFile, "recover_backup", DateTime.now().toString(DatePattern.PURE_DATETIME_FORMAT));
        //
        IPlugin plugin = PluginFactory.getPlugin("db-h2");
        Map<String, Object> map = new HashMap<>(10);
        map.put("dbName", this.getDbName());
        map.put("dbPath", dbLocalPathFile);
        map.put("backupPath", deleteBackup);
        plugin.execute("deleteDbFiles", map);
        // 清空记录
        StorageServiceFactory.clearExecuteSqlLog();
        return FileUtil.getAbsolutePath(deleteBackup);
    }

    /**
     * 转换 sql 文件内容,低版本兼容高版本
     *
     * @param sqlFile sql 文件
     */
    @Override
    public void transformSql(File sqlFile) {
        List<String> list = FileUtil.readLines(sqlFile, StandardCharsets.UTF_8);
        list = list.stream().map(s -> {
            if (StrUtil.startWith(s, "CREATE PRIMARY KEY SYSTEM_LOB_STREAM_PRIMARY_KEY ON SYSTEM_LOB_STREAM(ID, PART);")) {
                return "-- " + s;
            }
            return s;
        }).collect(Collectors.toList());
        FileUtil.writeLines(list, sqlFile, StandardCharsets.UTF_8);
    }


    /**
     * 恢复数据库
     *
     * @param dsFactory 数据库连接
     */
    @Override
    public void executeRecoverDbSql(DSFactory dsFactory, File recoverSqlFile) throws Exception {
        if (!FileUtil.isFile(recoverSqlFile)) {
            return;
        }
        //
        IPlugin plugin = PluginFactory.getPlugin("db-h2");
        Map<String, Object> map = new HashMap<>(10);
        map.put("backupSqlPath", FileUtil.getAbsolutePath(recoverSqlFile));
        map.put("dataSource", dsFactory.getDataSource());
        plugin.execute("restoreBackupSql", map);
    }


    @Override
    public void alterUser(String oldUes, String newUse, String newPwd) throws SQLException {
        String sql;
        if (StrUtil.equals(oldUes, newUse)) {
            sql = String.format("ALTER USER %s SET PASSWORD '%s' ", newUse, newPwd);
        } else {
            sql = String.format("create user %s password '%s';DROP USER %s", newUse, newPwd, oldUes);
        }
        Db.use(this.dsFactory.getDataSource()).execute(sql);
    }

    @Override
    public void backupSql(String url, String user, String pass, String backupSqlPath, List<String> tableNameList) throws Exception {
        Map<String, Object> map = new HashMap<>(10);
        map.put("url", url);
        map.put("user", user);
        map.put("pass", pass);
        map.put("backupSqlPath", backupSqlPath);
        map.put("tableNameList", tableNameList);
        IPlugin plugin = PluginFactory.getPlugin("db-h2");
        plugin.execute("backupSql", map);
    }

    @Override
    public void close() throws Exception {
        log.info("h2 db destroy");
        if (this.dsFactory != null) {
            dsFactory.destroy();
            this.dsFactory = null;
        }
    }
}
