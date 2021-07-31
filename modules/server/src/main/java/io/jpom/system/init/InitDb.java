package io.jpom.system.init;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.GlobalDSFactory;
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
		Setting setting = new Setting();
		DbConfig instance = DbConfig.getInstance();
		setting.set("url", instance.getDbUrl());
		setting.set("user", ServerExtConfigBean.getInstance().getDbUserName());
		setting.set("pass", ServerExtConfigBean.getInstance().getDbUserPwd());
		// 调试模式显示sql 信息
		if (JpomManifest.getInstance().isDebug()) {
			setting.set("showSql", "true");
			/**
			 * @author Hotstrip
			 * sql log only show when it's needed,
			 * if you want to check init sql,
			 * set the [sqlLevel] from [DEBUG] to [INFO]
			 */
			setting.set("sqlLevel", "DEBUG");
			setting.set("showParams", "true");
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
			String[] files = new String[] {
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
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("初始化数据库失败", e);
			System.exit(0);
			return;
		}
		instance.initOk();
		Console.log("h2 db inited");
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public void destroy() throws Exception {
		DSFactory dsFactory = GlobalDSFactory.get();
		dsFactory.close();
		Console.log("h2 db destroy");
	}
}
