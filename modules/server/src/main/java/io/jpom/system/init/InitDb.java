/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.GlobalDSFactory;
import cn.hutool.db.sql.SqlLog;
import cn.hutool.setting.Setting;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import io.jpom.common.JpomManifest;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.system.db.DbConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

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
		if (JpomManifest.getInstance().isDebug()) {

			setting.set(SqlLog.KEY_SHOW_SQL, "true");
			/**
			 * @author Hotstrip
			 * sql log only show when it's needed,
			 * if you want to check init sql,
			 * set the [sqlLevel] from [DEBUG] to [INFO]
			 */
			setting.set(SqlLog.KEY_SQL_LEVEL, "DEBUG");
			setting.set(SqlLog.KEY_SHOW_PARAMS, "true");
		}
		Console.log("start load h2 db");
		try {
			// 创建连接
			DSFactory dsFactory = DSFactory.create(setting);
			/**
			 * @author Hotstrip
			 * add another sql init file, if there are more sql file,
			 * please add it with same way
			 */
			String[] files = new String[]{
					"classpath:/bin/h2-db-v1.sql",
					"classpath:/bin/h2-db-v2.sql"
			};
			for (String sqlFile : files) {
				InputStream inputStream = ResourceUtil.getStream(sqlFile);
				String sql = IoUtil.read(inputStream, CharsetUtil.CHARSET_UTF_8);
				int rows = Db.use(dsFactory.getDataSource()).execute(sql);
				DefaultSystemLog.getLog().info("exec init SQL file: {} complete, and affected rows is: {}",
						sqlFile, rows);
			}
			DSFactory.setCurrentDSFactory(dsFactory);
			/**
			 * @author Hotstrip
			 * @date 2021-08-03
			 * load build.js data to DB
			 */
			LoadBuildJsonToDB.getInstance().doJsonToSql();
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("初始化数据库失败", e);
			System.exit(0);
			return;
		}
		instance.initOk();
		if (JpomManifest.getInstance().isDebug()) {
			Console.log("h2 db inited:" + dbUrl);
		} else {
			Console.log("h2 db inited");
			if (serverExtConfigBean.isH2ConsoleEnabled()
					&& StrUtil.equals(serverExtConfigBean.getDbUserName(), DbConfig.DEFAULT_USER_OR_PWD)
					&& StrUtil.equals(serverExtConfigBean.getDbUserPwd(), DbConfig.DEFAULT_USER_OR_PWD)) {
				Console.error("【安全警告】数据库账号密码使用默认的情况下不建议开启 h2 数据 web 控制台");
			}
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
		} catch (Exception ignored) {
		}
	}
}
