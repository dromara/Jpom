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
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.GlobalDSFactory;
import cn.hutool.db.sql.SqlLog;
import cn.hutool.setting.Setting;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.model.data.UserModel;
import io.jpom.service.h2db.BaseGroupService;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.system.WorkspaceService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.system.db.DbConfig;
import io.jpom.system.extconf.DbExtConfig;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 初始化数据库
 *
 * @author jiangzeyin
 * @since 2019/4/19
 */
@PreLoadClass(value = Integer.MIN_VALUE + 1)
@Configuration
@Slf4j
public class InitDb implements DisposableBean, InitializingBean {

    private static final List<Runnable> BEFORE_CALLBACK = new LinkedList<>();
    private static final List<Supplier<Boolean>> AFTER_CALLBACK = new LinkedList<>();

    public static void addBeforeCallback(Runnable consumer) {
        BEFORE_CALLBACK.add(consumer);
    }

    /**
     * 添加监听回调
     *
     * @param supplier 回调，返回 true 需要重新初始化数据库
     */
    public static void addCallback(Supplier<Boolean> supplier) {
        AFTER_CALLBACK.add(supplier);
    }

    @PreLoadMethod(value = Integer.MIN_VALUE)
    private static void init() {
        DbConfig instance = DbConfig.getInstance();
        ServerExtConfigBean serverExtConfigBean = ServerExtConfigBean.getInstance();
        DbExtConfig dbExtConfig = SpringUtil.getBean(DbExtConfig.class);
        //
        dbSecurityCheck(serverExtConfigBean, dbExtConfig);
        //
        BEFORE_CALLBACK.forEach(Runnable::run);
        //
        Setting setting = new Setting();
        String dbUrl = instance.getDbUrl();
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
        Console.log("start load h2 db");
        final String[] sqlFileNow = {StrUtil.EMPTY};
        try {
            // 创建连接
            DSFactory dsFactory = DSFactory.create(setting);
            // 先执行恢复数据
            instance.executeRecoverDbSql(dsFactory);
			/*
			  @author Hotstrip
			  add another sql init file, if there are more sql file,
			  please add it with same way
			 */
            PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath:/sql/*.sql");
            // 加载 sql 变更记录，避免重复执行
            Set<String> executeSqlLog = instance.loadExecuteSqlLog();
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
            instance.saveExecuteSqlLog(executeSqlLog);
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
            System.exit(0);
            return;
        }
        instance.initOk();
        // json load to db
        InitDb.loadJsonToDb();
        Console.log("h2 db Successfully loaded, url is 【{}】", dbUrl);
        syncAllNode();
    }

    private static void tryInitSql(List<Resource> resourcesList, Set<String> executeSqlLog, DataSource dataSource, Consumer<String> eachSql) {
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
     * @param serverExtConfigBean 服务端配置
     * @param dbExtConfig         外部配置
     */
    private static void dbSecurityCheck(ServerExtConfigBean serverExtConfigBean, DbExtConfig dbExtConfig) {
        if (!JpomManifest.getInstance().isDebug() && serverExtConfigBean.isH2ConsoleEnabled()
            && StrUtil.equals(dbExtConfig.getUserName(), DbConfig.DEFAULT_USER_OR_AUTHORIZATION)
            && StrUtil.equals(dbExtConfig.getUserPwd(), DbConfig.DEFAULT_USER_OR_AUTHORIZATION)) {
            JpomApplication.consoleExit(-2, "【安全警告】数据库账号密码使用默认的情况下不建议开启 h2 数据 web 控制台");
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

    private static void loadJsonToDb() {
		/*
		  @author Hotstrip
		  @since 2021-08-03
		  load build.js data to DB
		 */
        LoadBuildJsonToDB.getInstance().doJsonToSql();
        // @author bwcx_jzy @since 2021-12-02
        LoadJsonConfigToDb instance = LoadJsonConfigToDb.getInstance();
        // init workspace
        WorkspaceService workspaceService = SpringUtil.getBean(WorkspaceService.class);
        try {
            BaseServerController.resetInfo(UserModel.EMPTY);
            //
            instance.loadIpConfig();
            instance.loadMailConfig();
            instance.loadOutGivingWhitelistConfig();
            //
            instance.loadUserInfo();
            workspaceService.checkInitDefault();
            //
            instance.loadNodeInfo();
            instance.loadSshInfo();
            instance.loadMonitorInfo();
            instance.loadOutgivinInfo();
        } catch (JpomRuntimeException jpomRuntimeException) {
            Integer exitCode = jpomRuntimeException.getExitCode();
            if (exitCode != null) {
                Console.error(jpomRuntimeException.getMessage());
                System.exit(exitCode);
            } else {
                throw Lombok.sneakyThrow(jpomRuntimeException);
            }
        } finally {
            BaseServerController.removeEmpty();
        }
        //
        workspaceService.convertNullWorkspaceId();
        instance.convertMonitorLogField();
        //
        Map<String, BaseGroupService> groupServiceMap = SpringUtil.getApplicationContext().getBeansOfType(BaseGroupService.class);
        for (BaseGroupService<?> value : groupServiceMap.values()) {
            value.repairGroupFiled();
        }
    }

    private static void syncAllNode() {
        //  同步项目
        Map<String, BaseNodeService> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeService.class);
        for (BaseNodeService<?> value : beansOfType.values()) {
            value.syncAllNode();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
//        String[] signalArray = new String[]{"TERM"};
//        for (String s : signalArray) {
//            this.silenceSignalHandle(s);
//        }
    }

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
        DbConfig.getInstance().close();
        try {
            DSFactory dsFactory = GlobalDSFactory.get();
            GlobalDSFactory.set(null);
            dsFactory.destroy();
            Console.log("h2 db destroy");
        } catch (Throwable throwable) {
            //System.err.println(throwable.getMessage());
        }
    }

//    @Override
//    public void handle(Signal signal) {
//        log.warn("signal event {} {}", signal.getName(), signal.getNumber());
//        this.silenceDestroy();
//    }
}
