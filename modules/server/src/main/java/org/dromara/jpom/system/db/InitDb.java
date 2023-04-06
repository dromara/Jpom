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
import org.dromara.jpom.db.*;
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
 * @author jiangzeyin
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

    public InitDb(DbExtConfig dbExtConfig, BackupInfoService backupInfoService) {
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

    @SuppressWarnings("rawtypes")
    public void afterPropertiesSet(ApplicationContext applicationContext) {
        this.prepareCallback(applicationContext.getEnvironment());
        //
        log.debug("需要执行 {} 个前置事件", BEFORE_CALLBACK.size());
        BEFORE_CALLBACK.forEach(Runnable::run);
        IStorageService storageService = StorageServiceFactory.get();
        log.info("start load {} db", dbExtConfig.getMode());
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
                return StrUtil.equalsAnyIgnoreCase(modeType, "all", StorageServiceFactory.getMode().name());
            };
            List<Resource> resourceList = Arrays.stream(csvResources).filter(filter).collect(Collectors.toList());
            //
            Map<String, List<Resource>> listMap = CollStreamUtil.groupByKey(resourceList, resource -> {
                String filename = resource.getFilename();
                List<String> list = StrUtil.splitTrim(filename, StrUtil.DOT);
                return CollUtil.getFirst(list);
            });
            //
            Resource[] sqlResources = pathMatchingResourcePatternResolver.getResources("classpath*:/sql-view/*.sql");
            List<Resource> sqlResourceList = Arrays.stream(sqlResources).filter(filter).collect(Collectors.toList());
            listMap.put("execute", sqlResourceList);
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
            Set<String> executeSqlLog = StorageServiceFactory.loadExecuteSqlLog();
            tryInitSql(dbExtConfig.getMode(), listMap, executeSqlLog, dataSource, s -> sqlFileNow[0] = s);
            //
            StorageServiceFactory.saveExecuteSqlLog(executeSqlLog);
            // 执行回调方法
            log.debug("需要执行 {} 个回调", AFTER_CALLBACK.size());
            long count = AFTER_CALLBACK.entrySet()
                .stream()
                .mapToInt(value -> {
                    log.info("开始执行数据库事件：{}", value.getKey());
                    Supplier<Boolean> supplier = value.getValue();
                    boolean arg2 = supplier.get();
                    int code = arg2 ? 1 : 0;
                    log.info("数据库 {} 事件执行结束,：{}", value.getKey(), code);
                    return code;
                }).sum();
            if (count > 0) {
                // 因为导入数据后数据结构可能发生变动
                // 第二次初始化数据库
                tryInitSql(dbExtConfig.getMode(), listMap, CollUtil.newHashSet(), dataSource, s -> sqlFileNow[0] = s);
            }
            //
        } catch (Exception e) {
            log.error("初始化数据库失败 {}", sqlFileNow[0], e);
            JpomApplicationEvent.asyncExit(2);
            throw Lombok.sneakyThrow(e);
        }
        log.info("{} db Successfully loaded, url is 【{}】", storageService.mode(), storageService.dbUrl());
    }


    private void tryInitSql(DbExtConfig.Mode mode, Map<String, List<Resource>> listMap, Set<String> executeSqlLog, DataSource dataSource, Consumer<String> eachSql) {
        //
        Optional.ofNullable(listMap.get("table")).ifPresent(resources -> {
            for (Resource resource : resources) {
                String sql = StorageTableFactory.initTable(resource);
                this.executeSql(sql, resource.getFilename(), mode, executeSqlLog, dataSource, eachSql);
            }
        });
        Optional.ofNullable(listMap.get("execute")).ifPresent(resources -> {
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    String sql = IoUtil.readUtf8(inputStream);
                    this.executeSql(sql, resource.getFilename(), mode, executeSqlLog, dataSource, eachSql);
                } catch (Exception e) {
                    throw Lombok.sneakyThrow(e);
                }
            }
        });
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
            Db.use(dataSource).tx((CheckedUtil.VoidFunc1Rt<Db>) parameter -> {
                // 分隔后执行，mysql 不能执行多条 sql 语句
                List<String> list = StrUtil.isEmpty(sqlBuilderService.delimiter()) ?
                    CollUtil.newArrayList(sql) : StrUtil.splitTrim(sql, sqlBuilderService.delimiter());
                int rows = list.stream().mapToInt(value -> {
                    try {
                        return parameter.execute(value);
                    } catch (SQLException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                }).sum();
                log.info("exec init SQL file: {} complete, and affected rows is: {}", name, rows);
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
        IoUtil.close(StorageServiceFactory.get());
    }

    private void prepareCallback(Environment environment) {
        Opt.ofNullable(environment.getProperty("rest:load_init_db")).ifPresent(s -> {
            // 重新执行数据库初始化操作，一般用于手动修改数据库字段错误后，恢复默认的字段
            StorageServiceFactory.clearExecuteSqlLog();
        });
        Opt.ofNullable(environment.getProperty("recover:h2db")).ifPresent(s -> {
            // 恢复数据库，一般用于非正常关闭程序导致数据库奔溃，执行恢复数据逻辑
            try {
                this.recoverSqlFile = StorageServiceFactory.get().recoverDb();
            } catch (Exception e) {
                throw new JpomRuntimeException("Failed to restore database", e);
            }
        });
        Opt.ofNullable(environment.getProperty("backup-h2")).ifPresent(s -> {
            // 备份数据库
            this.addCallback("备份数据库", () -> {
                log.info("Start backing up the database");
                Future<BackupInfoModel> backupInfoModelFuture = backupInfoService.autoBackup();
                try {
                    BackupInfoModel backupInfoModel = backupInfoModelFuture.get();
                    log.info("Complete the backup database, save the path as {}", backupInfoModel.getFilePath());
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
                    String dbFiles = StorageServiceFactory.get().deleteDbFiles();
                    if (dbFiles != null) {
                        log.info("Automatically backup data files to {} path", dbFiles);
                    }
                } catch (Exception e) {
                    log.error("Failed to import according to sql,{}", s, e);
                }
            });
            // 导入数据
            importH2Sql(environment, s);
        });
        // 迁移数据
        Opt.ofNullable(environment.getProperty("h2-migrate-mysql")).ifPresent(s -> {
            DbExtConfig.Mode mode = dbExtConfig.getMode();
            if (mode != DbExtConfig.Mode.MYSQL) {
                throw new JpomRuntimeException(StrUtil.format("当前模式不正确，不能直接迁移到 {}", mode));
            }
            // 都提前清理
            StorageServiceFactory.clearExecuteSqlLog();
            //
            String user = environment.getProperty("h2-user");
            String url = environment.getProperty("h2-url");
            String pass = environment.getProperty("h2-pass");
            this.addCallback("迁移数据", () -> {
                //
                StorageServiceFactory.migrateH2ToNow(dbExtConfig, url, user, pass);
                return false;
            });
            log.info("开始等待数据迁移");
        });
    }

    private void importH2Sql(Environment environment, String importH2Sql) {
        this.addCallback("导入数据", () -> {
            File file = FileUtil.file(importH2Sql);
            String sqlPath = FileUtil.getAbsolutePath(file);
            if (!FileUtil.isFile(file)) {
                throw new JpomRuntimeException(StrUtil.format("sql file does not exist :{}", sqlPath));
            }
            //
            Opt.ofNullable(environment.getProperty("transform-sql")).ifPresent(s -> StorageServiceFactory.get().transformSql(file));
            //
            log.info("Start importing data:{}", sqlPath);
            boolean flag = backupInfoService.restoreWithSql(sqlPath);
            if (!flag) {
                throw new JpomRuntimeException(StrUtil.format("Failed to import according to sql,{}", sqlPath));
            }
            log.info("Import successfully according to sql,{}", sqlPath);
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
