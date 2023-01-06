/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package top.jpom.storage;

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
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.system.JpomRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import org.h2.jdbc.JdbcSQLNonTransientException;
import org.h2.mvstore.MVStoreException;
import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.util.Assert;
import top.jpom.db.DbExtConfig;
import top.jpom.db.IStorageService;
import top.jpom.db.StorageServiceFactory;

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
        Assert.hasText(this.dbUrl, "还没有初始化数据库");
        return dbUrl;
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
        Assert.isNull(this.dsFactory, "不要重复初始化数据库");
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
        Assert.notNull(this.dsFactory, "还没有初始化数据库");
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
            throw new JpomRuntimeException("【安全警告】数据库账号密码使用默认的情况下不建议开启 h2 数据 web 控制台");
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
                String tip = "升级数据库流程：" + StrUtil.LF;
                tip += StrUtil.TAB + "1. 导出低版本数据 【启动程序参数里面添加 --backup-h2】" + StrUtil.LF;
                tip += StrUtil.TAB + "2. 将导出的低版本数据( sql 文件) 导入到新版本中【启动程序参数里面添加 --replace-import-h2-sql=/xxxx.sql (路径需要替换为第一步控制台输出的 sql 文件保存路径)】";
                return new JpomRuntimeException("数据库版本不兼容,需要处理跨版本升级。" + StrUtil.LF + tip + StrUtil.LF, -1);
            }
        }
        if (e instanceof JdbcSQLNonTransientException || ExceptionUtil.isCausedBy(e, JdbcSQLNonTransientException.class)) {
            return new JpomRuntimeException("数据库异常,可能数据库文件已经损坏(可能丢失部分数据),需要重新初始化。可以尝试在启动参数里面添加 --recover:h2db 来自动恢复,：" + message, e);
        }
        if (e instanceof JdbcSQLNonTransientConnectionException) {
            return new JpomRuntimeException("数据库异常,可能因为服务器资源不足（内存、硬盘）等原因造成数据异常关闭。需要手动重启服务端来恢复，：" + message, e);
        }
        return new JpomRuntimeException("数据库异常", e);
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
