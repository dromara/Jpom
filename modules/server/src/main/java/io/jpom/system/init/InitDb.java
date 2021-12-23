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
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.JpomManifest;
import io.jpom.model.data.UserModel;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.system.WorkspaceService;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.system.db.DbConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 初始化数据库
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@PreLoadClass(-1)
@Configuration
public class InitDb implements DisposableBean, InitializingBean {

	@PreLoadMethod
	private static void init() {
		//
		DbConfig instance = DbConfig.getInstance();
		ServerExtConfigBean serverExtConfigBean = ServerExtConfigBean.getInstance();
		//
		Setting setting = new Setting();
		String dbUrl = instance.getDbUrl();
		setting.set("url", dbUrl);
		setting.set("user", serverExtConfigBean.getDbUserName());
		setting.set("pass", serverExtConfigBean.getDbUserPwd());
		// 配置连接池大小
		setting.set("maxActive", "50");
		setting.set("initialSize", "1");
		setting.set("maxWait", "10");
		setting.set("minIdle", "1");
		// 调试模式显示sql 信息
		if (!ConfigBean.getInstance().isPro()) {

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
		String sqlFileNow = StrUtil.EMPTY;
		try {
			// 创建连接
			DSFactory dsFactory = DSFactory.create(setting);
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
			for (Resource resource : resourcesList) {
				try (InputStream inputStream = resource.getInputStream()) {
					String sql = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
					String sha1 = SecureUtil.sha1(sql);
					if (executeSqlLog.contains(sha1)) {
						// 已经执行过啦，不再执行
						continue;
					}
					sqlFileNow = resource.getFilename();
					int rows = Db.use(dsFactory.getDataSource()).execute(sql);
					DefaultSystemLog.getLog().info("exec init SQL file: {} complete, and affected rows is: {}", sqlFileNow, rows);
					executeSqlLog.add(sha1);
				} catch (IOException ignored) {
				}
			}
			instance.saveExecuteSqlLog(executeSqlLog);
			DSFactory.setCurrentDSFactory(dsFactory);
			//
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("初始化数据库失败 {}", sqlFileNow, e);
			System.exit(0);
			return;
		}
		instance.initOk();
		// json load to db
		InitDb.loadJsonToDb();
		Console.log("h2 db Successfully loaded, url is 【{}】", dbUrl);
		if (JpomManifest.getInstance().isDebug()) {
			//
		} else {
			if (serverExtConfigBean.isH2ConsoleEnabled()
					&& StrUtil.equals(serverExtConfigBean.getDbUserName(), DbConfig.DEFAULT_USER_OR_AUTHORIZATION)
					&& StrUtil.equals(serverExtConfigBean.getDbUserPwd(), DbConfig.DEFAULT_USER_OR_AUTHORIZATION)) {
				Console.error("【安全警告】数据库账号密码使用默认的情况下不建议开启 h2 数据 web 控制台");
				System.exit(-2);
			}
		}
	}

	private static void loadJsonToDb() {
		/*
		  @author Hotstrip
		  @date 2021-08-03
		  load build.js data to DB
		 */
		LoadBuildJsonToDB.getInstance().doJsonToSql();
		// @author bwcx_jzy @date 2021-12-02
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
		} finally {
			BaseServerController.remove();
		}
		//
		workspaceService.convertNullWorkspaceId();
		instance.convertMonitorLogField();
		//  同步项目
		Map<String, BaseNodeService> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeService.class);
		for (BaseNodeService<?> value : beansOfType.values()) {
			value.syncAllNode();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public void destroy() throws Exception {
		try {
			DSFactory dsFactory = GlobalDSFactory.get();
			dsFactory.destroy();
			Console.log("h2 db destroy");
		} catch (Throwable ignored) {
		}
	}
}
