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
package io.jpom.system.init;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.CheckedUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.GlobalDSFactory;
import cn.hutool.db.sql.SqlLog;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.setting.Setting;
import io.jpom.common.JpomManifest;
import io.jpom.model.data.BackupInfoModel;
import io.jpom.service.dblog.BackupInfoService;
import io.jpom.service.h2db.BaseGroupService;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.system.db.DbConfig;
import io.jpom.system.extconf.DbExtConfig;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 初始化数据库
 *
 * @author jiangzeyin
 * @since 2019/4/19
 */
@Configuration
@Slf4j
public class InitDb implements DisposableBean, ApplicationContextAware {

    private final List<Runnable> BEFORE_CALLBACK = new LinkedList<>();
    private final List<Supplier<Boolean>> AFTER_CALLBACK = new LinkedList<>();

    /**
     * author Hotstrip
     * 是否开启 web 访问数据库
     *
     * @see <a href=http://${ip}:${port}/h2-console>http://${ip}:${port}/h2-console</a>
     */
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    public InitDb(DbConfig dbConfig,
                  DbExtConfig dbExtConfig) {
        this.dbConfig = dbConfig;
        this.dbExtConfig = dbExtConfig;
    }

    public void addBeforeCallback(Runnable consumer) {
        BEFORE_CALLBACK.add(consumer);
    }

    /**
     * 添加监听回调
     *
     * @param supplier 回调，返回 true 需要重新初始化数据库
     */
    public void addCallback(Supplier<Boolean> supplier) {
        AFTER_CALLBACK.add(supplier);
    }

    private final DbConfig dbConfig;
    private final DbExtConfig dbExtConfig;

    private void init() {
        //
        dbSecurityCheck(dbExtConfig);
        //
        BEFORE_CALLBACK.forEach(Runnable::run);
        //
        Setting setting = new Setting();
        String dbUrl = dbConfig.getDbUrl();
        setting.set("url", dbUrl);
        setting.set("user", dbExtConfig.getUserName());
        setting.set("pass", dbExtConfig.getUserPwd());
        // 配置连接池大小
        setting.set("maxActive", dbExtConfig.getMaxActive() + "");
        setting.set("initialSize", dbExtConfig.getInitialSize() + "");
        setting.set("maxWait", dbExtConfig.getMaxWait() + "");
        setting.set("minIdle", dbExtConfig.getMinIdle() + "");
        // 调试模式显示sql 信息
        if (dbExtConfig.getShowSql()) {

            setting.set(SqlLog.KEY_SHOW_SQL, "true");
			/*
			  @author Hotstrip
			  sql log only show when it's needed,
			  if you want to check init sql,
			  set the [sqlLevel] from [DEBUG] to [INFO]
			 */
            setting.set(SqlLog.KEY_SQL_LEVEL, "DEBUG");
            setting.set(SqlLog.KEY_SHOW_PARAMS, "true");
        }
        log.info("start load h2 db");
        final String[] sqlFileNow = {StrUtil.EMPTY};
        try {
            // 创建连接
            DSFactory dsFactory = DSFactory.create(setting);
            // 先执行恢复数据
            dbConfig.executeRecoverDbSql(dsFactory);
			/*
			  @author Hotstrip
			  add another sql init file, if there are more sql file,
			  please add it with same way
			 */
            PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath:/sql/*.sql");
            // 加载 sql 变更记录，避免重复执行
            Set<String> executeSqlLog = dbConfig.loadExecuteSqlLog();
            // 过滤 temp sql
            List<Resource> resourcesList = Arrays.stream(resources)
                .sorted((o1, o2) -> StrUtil.compare(o1.getFilename(), o2.getFilename(), true))
                .filter(resource -> !StrUtil.containsIgnoreCase(resource.getFilename(), "temp"))
                .collect(Collectors.toList());
            // 遍历
            DataSource dataSource = dsFactory.getDataSource();
            // 第一次初始化数据库
            tryInitSql(resourcesList, executeSqlLog, dataSource, s -> sqlFileNow[0] = s);
            //
            dbConfig.saveExecuteSqlLog(executeSqlLog);
            GlobalDSFactory.set(dsFactory);
            // 执行回调方法
            long count = AFTER_CALLBACK.stream().mapToInt(value -> value.get() ? 1 : 0).count();
            if (count > 0) {
                // 因为导入数据后数据结构可能发生变动
                // 第二次初始化数据库
                tryInitSql(resourcesList, CollUtil.newHashSet(), dataSource, s -> sqlFileNow[0] = s);
            }
            //
        } catch (Exception e) {
            log.error("初始化数据库失败 {}", sqlFileNow[0], e);
            throw Lombok.sneakyThrow(e);
        }
        dbConfig.initOk();
        log.info("h2 db Successfully loaded, url is 【{}】", dbUrl);
        this.loadJsonToDb();
        this.syncAllNode();
    }

    private void tryInitSql(List<Resource> resourcesList, Set<String> executeSqlLog, DataSource dataSource, Consumer<String> eachSql) {
        for (Resource resource : resourcesList) {
            try (InputStream inputStream = resource.getInputStream()) {
                String sql = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
                String sha1 = SecureUtil.sha1(sql);
                if (executeSqlLog.contains(sha1)) {
                    // 已经执行过啦，不再执行
                    continue;
                }
                String sqlFileNow = resource.getFilename();
                eachSql.accept(sqlFileNow);
                Db.use(dataSource).tx((CheckedUtil.VoidFunc1Rt<Db>) parameter -> {
                    try {
                        int rows = parameter.execute(sql);
                        log.info("exec init SQL file: {} complete, and affected rows is: {}", sqlFileNow, rows);
                    } catch (SQLException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                });
                executeSqlLog.add(sha1);
            } catch (Exception e) {
                throw Lombok.sneakyThrow(e);
            }
        }
    }

    /**
     * 数据库是否开启 web 配置检查
     *
     * @param dbExtConfig 外部配置
     */
    private void dbSecurityCheck(DbExtConfig dbExtConfig) {
        if (!JpomManifest.getInstance().isDebug() && h2ConsoleEnabled
            && StrUtil.equals(dbExtConfig.getUserName(), DbConfig.DEFAULT_USER_OR_AUTHORIZATION)
            && StrUtil.equals(dbExtConfig.getUserPwd(), DbConfig.DEFAULT_USER_OR_AUTHORIZATION)) {
            log.error("【安全警告】数据库账号密码使用默认的情况下不建议开启 h2 数据 web 控制台");
            System.exit(-2);
        }
    }

    /**
     * 修改账号 密码
     *
     * @param oldUes 旧的账号
     * @param newUse 新的账号
     * @param newPwd 新密码
     */
    public void alterUser(String oldUes, String newUse, String newPwd) throws SQLException {
        String sql;
        if (StrUtil.equals(oldUes, newUse)) {
            sql = String.format("ALTER USER %s SET PASSWORD '%s' ", newUse, newPwd);
        } else {
            sql = String.format("create user %s password '%s';DROP USER %s", newUse, newPwd, oldUes);
        }
        Db.use().execute(sql);
    }

    private void loadJsonToDb() {
        //
        Map<String, BaseGroupService> groupServiceMap = SpringUtil.getApplicationContext().getBeansOfType(BaseGroupService.class);
        for (BaseGroupService<?> value : groupServiceMap.values()) {
            value.repairGroupFiled();
        }
    }

    private void syncAllNode() {
        //  同步项目
        Map<String, BaseNodeService> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeService.class);
        for (BaseNodeService<?> value : beansOfType.values()) {
            value.syncAllNode();
        }
    }

//    @Override
//    public void afterPropertiesSet() throws Exception {
////        String[] signalArray = new String[]{"TERM"};
////        for (String s : signalArray) {
////            this.silenceSignalHandle(s);
////        }
//
//    }

    @Override
    public void destroy() throws Exception {
        this.silenceDestroy();
    }

//    private void silenceSignalHandle(String name) {
//        try {
//            Signal.handle(new Signal(name), this);
//            log.debug("{} signal handle success", name);
//        } catch (Exception e) {
//            log.debug("{} signal handle fail:{}", name, e.getMessage());
//        }
//    }

    private void silenceDestroy() {
        dbConfig.close();
        try {
            DSFactory dsFactory = GlobalDSFactory.get();
            GlobalDSFactory.set(null);
            dsFactory.destroy();
            log.info("h2 db destroy");
        } catch (Throwable throwable) {
            //System.err.println(throwable.getMessage());
        }
    }

    private void prepareCallback(Environment environment) {
        Opt.ofBlankAble(environment.getProperty("rest:load_init_db")).ifPresent(s -> {
            // 重新执行数据库初始化操作，一般用于手动修改数据库字段错误后，恢复默认的字段
            dbConfig.clearExecuteSqlLog();
        });
        Opt.ofBlankAble(environment.getProperty("recover:h2db")).ifPresent(s -> {
            // 恢复数据库，一般用于非正常关闭程序导致数据库奔溃，执行恢复数据逻辑
            try {
                dbConfig.recoverDb();
            } catch (Exception e) {
                log.error("Failed to restore database：{}", e.getMessage(), e);
                System.exit(-2);
            }
        });
        Opt.ofBlankAble(environment.getProperty("backup-h2")).ifPresent(s -> {
            // 备份数据库
            this.addCallback(() -> {
                log.info("Start backing up the database");
                BackupInfoService backupInfoService = SpringUtil.getBean(BackupInfoService.class);
                Future<BackupInfoModel> backupInfoModelFuture = backupInfoService.autoBackup();
                try {
                    BackupInfoModel backupInfoModel = backupInfoModelFuture.get();
                    log.info("Complete the backup database, save the path as {}", backupInfoModel.getFilePath());
                    System.exit(0);
                } catch (Exception e) {
                    log.error("Backup database failed：{}", e.getMessage(), e);
                    System.exit(-2);
                }
                return false;
            });
        });
        // 导入数据
        Opt.ofBlankAble(environment.getProperty("import-h2-sql")).ifPresent(this::importH2Sql);
        Opt.ofBlankAble(environment.getProperty("replace-import-h2-sql")).ifPresent(s -> {
            // 删除掉旧数据
            this.addBeforeCallback(() -> {
                try {
                    String dbFiles = dbConfig.deleteDbFiles();
                    if (dbFiles != null) {
                        log.info("Automatically backup data files to {} path", dbFiles);
                    }
                } catch (Exception e) {
                    log.error("Failed to import according to sql,{}", s, e);
                }
            });
            // 导入数据
            importH2Sql(s);
        });
    }

    private void importH2Sql(String importH2Sql) {
        Environment environment = SpringUtil.getApplicationContext().getEnvironment();
        this.addCallback(() -> {
            File file = FileUtil.file(importH2Sql);
            String sqlPath = FileUtil.getAbsolutePath(file);
            if (!FileUtil.isFile(file)) {
                log.error("sql file does not exist :{}", sqlPath);
                System.exit(2);
            }
            //
            Opt.ofBlankAble(environment.getProperty("transform-sql")).ifPresent(s -> dbConfig.transformSql(file));
            //
            log.info("Start importing data:{}", sqlPath);
            BackupInfoService backupInfoService = SpringUtil.getBean(BackupInfoService.class);
            boolean flag = backupInfoService.restoreWithSql(sqlPath);
            if (!flag) {
                log.error("Failed to import according to sql,{}", sqlPath);
                System.exit(2);
            }
            log.info("Import successfully according to sql,{}", sqlPath);
            return true;
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.prepareCallback(applicationContext.getEnvironment());
        this.init();
    }

//    @Override
//    public void handle(Signal signal) {
//        log.warn("signal event {} {}", signal.getName(), signal.getNumber());
//        this.silenceDestroy();
//    }
}
