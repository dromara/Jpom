/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system.db;

import cn.hutool.cache.GlobalPruneTimer;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.CheckedUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.JpomApplicationEvent;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.*;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.model.data.BackupInfoModel;
import org.dromara.jpom.service.dblog.BackupInfoService;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 初始化数据库
 *
 * @author bwcx_jzy
 * @since 2019/4/19
 */
@Configuration
@Slf4j
public class InitDb implements DisposableBean, ILoadEvent {

    private final List<Runnable> BEFORE_CALLBACK = new LinkedList<>();
    private final Map<String, Supplier<Boolean>> AFTER_CALLBACK = new LinkedHashMap<>();
    private final DbExtConfig dbExtConfig;
    private final BackupInfoService backupInfoService;
    /**
     * 恢复 sql 文件
     */
    private File recoverSqlFile;

    public InitDb(DbExtConfig dbExtConfig,
                  BackupInfoService backupInfoService) {
        this.dbExtConfig = dbExtConfig;
        this.backupInfoService = backupInfoService;
    }

    public void addBeforeCallback(Runnable consumer) {
        BEFORE_CALLBACK.add(consumer);
    }

    /**
     * 添加监听回调
     *
     * @param name     事件名称
     * @param supplier 回调，返回 true 需要重新初始化数据库
     */
    public void addCallback(String name, Supplier<Boolean> supplier) {
        AFTER_CALLBACK.put(name, supplier);
    }

    public void afterPropertiesSet(ApplicationContext applicationContext) {
        this.prepareCallback(applicationContext.getEnvironment());
        //
        log.debug(I18nMessageUtil.get("i18n.need_execute_pre_events.b848"), BEFORE_CALLBACK.size());
        BEFORE_CALLBACK.forEach(Runnable::run);
        IStorageService storageService = StorageServiceFactory.getInstance().get();
        log.info(I18nMessageUtil.get("i18n.start_loading_database.b040"), dbExtConfig.getMode());
        DSFactory dsFactory = storageService.init(dbExtConfig);
        final String[] sqlFileNow = {StrUtil.EMPTY};
        try {
            // 先执行恢复数据
            storageService.executeRecoverDbSql(dsFactory, this.recoverSqlFile);
            //
            PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] csvResources = pathMatchingResourcePatternResolver.getResources("classpath*:/sql-view/*.csv");
            Predicate<Resource> filter = resource -> {
                String filename = resource.getFilename();
                List<String> list = StrUtil.splitTrim(filename, StrUtil.DOT);
                String modeType = CollUtil.get(list, 1);
                return StrUtil.equalsAnyIgnoreCase(modeType, "all", StorageServiceFactory.getInstance().getMode().name());
            };
            List<Resource> resourceList = Arrays.stream(csvResources).filter(filter).collect(Collectors.toList());
            //
            Map<String, List<Resource>> listMap = CollStreamUtil.groupByKey(resourceList, resource -> {
                String filename = resource.getFilename();
                List<String> list = StrUtil.splitTrim(filename, StrUtil.DOT);
                return CollUtil.getFirst(list);
            });
            //
            {
                Resource[] sqlResources = pathMatchingResourcePatternResolver.getResources("classpath*:/sql-view/execute.*.sql");
                List<Resource> sqlResourceList = Arrays.stream(sqlResources).filter(filter).collect(Collectors.toList());
                listMap.put("execute", sqlResourceList);
            }
            {
                Resource[] sqlResources = pathMatchingResourcePatternResolver.getResources("classpath*:/sql-view/init.*.sql");
                List<Resource> sqlResourceList = Arrays.stream(sqlResources).filter(filter).collect(Collectors.toList());
                listMap.put("init", sqlResourceList);
            }
            //
            for (Map.Entry<String, List<Resource>> entry : listMap.entrySet()) {
                List<Resource> value = entry.getValue();
                // 排序,先后顺序执行
                value.sort((o1, o2) -> StrUtil.compare(o1.getFilename(), o2.getFilename(), true));
                entry.setValue(value);
            }
            // 遍历
            DataSource dataSource = dsFactory.getDataSource();
            // 第一次初始化数据库
            // 加载 sql 变更记录，避免重复执行
            Set<String> executeSqlLog = StorageServiceFactory.getInstance().loadExecuteSqlLog();
            tryInitSql(dbExtConfig.getMode(), listMap, executeSqlLog, dataSource, s -> sqlFileNow[0] = s);
            //
            StorageServiceFactory.getInstance().saveExecuteSqlLog(executeSqlLog);
            // 执行回调方法
            log.debug(I18nMessageUtil.get("i18n.need_execute_callbacks.b708"), AFTER_CALLBACK.size());
            long count = AFTER_CALLBACK.entrySet()
                .stream()
                .mapToInt(value -> {
                    log.info(I18nMessageUtil.get("i18n.start_executing_database_event.fc57"), value.getKey());
                    Supplier<Boolean> supplier = value.getValue();
                    boolean arg2 = supplier.get();
                    int code = arg2 ? 1 : 0;
                    log.info(I18nMessageUtil.get("i18n.database_event_execution_ended.690b"), value.getKey(), code);
                    return code;
                }).sum();
            if (count > 0) {
                // 因为导入数据后数据结构可能发生变动
                // 第二次初始化数据库
                tryInitSql(dbExtConfig.getMode(), listMap, CollUtil.newHashSet(), dataSource, s -> sqlFileNow[0] = s);
            }
            //
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.initialize_database_failure.2ef9"), sqlFileNow[0], e);
            JpomApplicationEvent.asyncExit(2);
            throw Lombok.sneakyThrow(e);
        }
        String s = "数据库加载成功，URL为";
        log.info("{} {}：[{}]", storageService.mode(), s, storageService.dbUrl());
    }


    private void tryInitSql(DbExtConfig.Mode mode, Map<String, List<Resource>> listMap, Set<String> executeSqlLog, DataSource dataSource, Consumer<String> eachSql) {
        Consumer<List<Resource>> consumer = resources -> {
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    String sql = IoUtil.readUtf8(inputStream);
                    this.executeSql(sql, resource.getFilename(), mode, executeSqlLog, dataSource, eachSql);
                } catch (Exception e) {
                    throw Lombok.sneakyThrow(e);
                }
            }
        };
        // 初始化sql
        Optional.ofNullable(listMap.get("init")).ifPresent(consumer);
        //
        Optional.ofNullable(listMap.get("table")).ifPresent(resources -> {
            for (Resource resource : resources) {
                String sql = StorageTableFactory.initTable(resource);
                this.executeSql(sql, resource.getFilename(), mode, executeSqlLog, dataSource, eachSql);
            }
        });
        //
        Optional.ofNullable(listMap.get("execute")).ifPresent(consumer);
        //
        Optional.ofNullable(listMap.get("alter")).ifPresent(resources -> {
            for (Resource resource : resources) {
                String sql = StorageTableFactory.initAlter(resource);
                this.executeSql(sql, resource.getFilename(), mode, executeSqlLog, dataSource, eachSql);
            }
        });
        Optional.ofNullable(listMap.get("index")).ifPresent(resources -> {
            for (Resource resource : resources) {
                String sql = StorageTableFactory.initIndex(resource);
                this.executeSql(sql, resource.getFilename(), mode, executeSqlLog, dataSource, eachSql);
            }
        });
    }

    private void executeSql(String sql, String name, DbExtConfig.Mode mode, Set<String> executeSqlLog, DataSource dataSource, Consumer<String> eachSql) {
        String sha1 = SecureUtil.sha1(sql + mode);
        if (executeSqlLog.contains(sha1)) {
            // 已经执行过啦，不再执行
            return;
        }
        eachSql.accept(name);
        try {
            IStorageSqlBuilderService sqlBuilderService = StorageTableFactory.get();
            Db.use(dataSource, DialectUtil.getDialectByMode(mode)).tx((CheckedUtil.VoidFunc1Rt<Db>) parameter -> {
                // 分隔后执行，mysql 不能执行多条 sql 语句
                List<String> list = StrUtil.isEmpty(sqlBuilderService.delimiter()) ?
                    CollUtil.newArrayList(sql) : StrUtil.splitTrim(sql, sqlBuilderService.delimiter());
                int rows = list.stream()
                    .mapToInt(value -> {
                        try {
                            return parameter.execute(value);
                        } catch (SQLException e) {
                            log.warn(I18nMessageUtil.get("i18n.error_sql.15ff"), value);
                            throw Lombok.sneakyThrow(e);
                        }
                    })
                    .sum();
                String s = "执行初始化SQL文件";
                String s1 = "影响行数";
                log.info("{}：{}，{}：{}", s, name, s1, rows);
            });
        } catch (SQLException e) {
            throw Lombok.sneakyThrow(e);
        }
        executeSqlLog.add(sha1);
    }

    @Override
    public void destroy() throws Exception {
        // 需要优先关闭线程池，避免异常更新数据的逻辑没有释放
        JpomApplication.shutdownGlobalThreadPool();
        //
        GlobalPruneTimer.INSTANCE.shutdownNow();
        // 关闭数据库
        IoUtil.close(StorageServiceFactory.getInstance().get());
    }

    private void prepareCallback(Environment environment) {
        Opt.ofNullable(environment.getProperty("rest:load_init_db")).ifPresent(s -> {
            // 重新执行数据库初始化操作，一般用于手动修改数据库字段错误后，恢复默认的字段
            StorageServiceFactory.getInstance().clearExecuteSqlLog();
        });
        Opt.ofNullable(environment.getProperty("recover:h2db")).ifPresent(s -> {
            // 恢复数据库，一般用于非正常关闭程序导致数据库奔溃，执行恢复数据逻辑
            try {
                this.recoverSqlFile = StorageServiceFactory.getInstance().get().recoverDb();
            } catch (Exception e) {
                throw new JpomRuntimeException("Failed to restore database", e);
            }
        });
        Opt.ofNullable(environment.getProperty("backup-h2")).ifPresent(s -> {
            // 备份数据库
            this.addCallback(I18nMessageUtil.get("i18n.backup_database.9524"), () -> {
                log.info("开始备份数据库");
                Future<BackupInfoModel> backupInfoModelFuture = backupInfoService.autoBackup();
                try {
                    BackupInfoModel backupInfoModel = backupInfoModelFuture.get();
                    String s1 = "数据库备份完成，保存路径为";
                    log.info("{}：{}", s1, backupInfoModel.getFilePath());
                } catch (Exception e) {
                    throw new JpomRuntimeException(StrUtil.format("Backup database failed：{}", e.getMessage()), e);
                }
                return false;
            });
        });
        // 导入数据
        Opt.ofBlankAble(environment.getProperty("import-h2-sql")).ifPresent(s -> importH2Sql(environment, s));
        Opt.ofBlankAble(environment.getProperty("replace-import-h2-sql")).ifPresent(s -> {
            // 删除掉旧数据
            this.addBeforeCallback(() -> {
                try {
                    String dbFiles = StorageServiceFactory.getInstance().get().deleteDbFiles();
                    if (dbFiles != null) {
                        String s1 = "自动备份数据文件到路径";
                        log.info("{}：{}", s1, dbFiles);
                    }
                } catch (Exception e) {
                    log.error("Failed to import according to sql,{}", s, e);
                }
            });
            // 导入数据
            importH2Sql(environment, s);
        });

        // 迁移数据
        Consumer<DbExtConfig.Mode> migrateOpr = targetMode -> {
            DbExtConfig.Mode mode = dbExtConfig.getMode();
            if (mode != targetMode) {
                throw new JpomRuntimeException(StrUtil.format(I18nMessageUtil.get("i18n.incorrect_mode_for_migration.caef"), targetMode));
            }
            // 都提前清理
            StorageServiceFactory.getInstance().clearExecuteSqlLog();
            //
            String user = environment.getProperty("h2-user");
            String url = environment.getProperty("h2-url");
            String pass = environment.getProperty("h2-pass");
            this.addCallback(I18nMessageUtil.get("i18n.migrate_data.f556"), () -> {
                //
                StorageServiceFactory.getInstance().migrateH2ToNow(dbExtConfig, url, user, pass, targetMode);
                return false;
            });
            log.info(I18nMessageUtil.get("i18n.start_waiting_for_data_migration.e76f"));
        };
        Opt.ofNullable(environment.getProperty("h2-migrate-mysql")).ifPresent(s -> {
            migrateOpr.accept(DbExtConfig.Mode.MYSQL);
        });
        Opt.ofNullable(environment.getProperty("h2-migrate-postgresql")).ifPresent(s -> {
            migrateOpr.accept(DbExtConfig.Mode.POSTGRESQL);
        });
        Opt.ofNullable(environment.getProperty("h2-migrate-mariadb")).ifPresent(s -> {
            migrateOpr.accept(DbExtConfig.Mode.MARIADB);
        });
    }

    private void importH2Sql(Environment environment, String importH2Sql) {
        this.addCallback(I18nMessageUtil.get("i18n.import_data.8ef8"), () -> {
            File file = FileUtil.file(importH2Sql);
            String sqlPath = FileUtil.getAbsolutePath(file);
            if (!FileUtil.isFile(file)) {
                throw new JpomRuntimeException(StrUtil.format("sql file does not exist :{}", sqlPath));
            }
            //
            Opt.ofNullable(environment.getProperty("transform-sql")).ifPresent(s -> StorageServiceFactory.getInstance().get().transformSql(file));
            //
            log.info("开始导入数据：{}", sqlPath);
            boolean flag = backupInfoService.restoreWithSql(sqlPath);
            if (!flag) {
                throw new JpomRuntimeException(StrUtil.format("Failed to import according to sql,{}", sqlPath));
            }
            log.info("导入成功：{}", sqlPath);
            return true;
        });
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
//    @Override
//    public void handle(Signal signal) {
//        log.warn("signal event {} {}", signal.getName(), signal.getNumber());
//        this.silenceDestroy();
//    }
}
