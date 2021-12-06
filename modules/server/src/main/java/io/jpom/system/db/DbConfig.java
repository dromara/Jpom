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
package io.jpom.system.db;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.JpomApplication;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.ServerExtConfigBean;

import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 数据库配置
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class DbConfig {

	private static final String DB = "db";

	/**
	 * 默认的账号或者密码
	 */
	public static final String DEFAULT_USER_OR_AUTHORIZATION = "jpom";

	private static DbConfig dbConfig;

	/**
	 * 是否初始化成功
	 */
	private volatile boolean init;

	/**
	 * 单利模式
	 *
	 * @return config
	 */
	public static DbConfig getInstance() {
		if (dbConfig == null) {
			dbConfig = new DbConfig();
		}
		return dbConfig;
	}

	public void initOk() {
		init = true;
	}

	public boolean isInit() {
		return init;
	}

	/**
	 * 获取数据库保存路径
	 *
	 * @return 默认本地数据目录下面的 db 目录
	 * @author bwcx_jzy
	 */
	public File dbLocalPath() {
		return FileUtil.file(ExtConfigBean.getInstance().getPath(), DB);
	}

	/**
	 * 获取数据库的jdbc 连接
	 *
	 * @return jdbc
	 */
	public String getDbUrl() {
		File file = FileUtil.file(DbConfig.getInstance().dbLocalPath(), JpomApplication.getAppType().name());
		String path = FileUtil.getAbsolutePath(file);
		return StrUtil.format("jdbc:h2:{};CACHE_SIZE={}", path, ServerExtConfigBean.getInstance().getCacheSize().toKilobytes());
	}

	/**
	 * 加载 本地已经执行的记录
	 *
	 * @return sha1 log
	 * @author bwcx_jzy
	 */
	public Set<String> loadExecuteSqlLog() {
		File localPath = this.dbLocalPath();
		File file = FileUtil.file(localPath, "execute.init.sql.log");
		if (!FileUtil.isFile(file)) {
			// 不存在或者是文件夹
			FileUtil.del(file);
			return new LinkedHashSet<>();
		}
		List<String> strings = FileUtil.readLines(file, CharsetUtil.CHARSET_UTF_8);
		return new LinkedHashSet<>(strings);
	}

	/**
	 * 清除执行记录
	 */
	public void clearExecuteSqlLog() {
		File localPath = this.dbLocalPath();
		File file = FileUtil.file(localPath, "execute.init.sql.log");
		FileUtil.del(file);
	}

	/**
	 * 保存本地已经执行的记录
	 *
	 * @author bwcx_jzy
	 */
	public void saveExecuteSqlLog(Set<String> logs) {
		File localPath = this.dbLocalPath();
		File file = FileUtil.file(localPath, "execute.init.sql.log");
		FileUtil.writeUtf8Lines(logs, file);
	}

	/**
	 * 清除超限制数量的数据
	 *
	 * @param tableName 表名
	 * @param timeClo   时间字段名
	 */
	public static void autoClear(String tableName, String timeClo) {
		if (ServerExtConfigBean.getInstance().getH2DbLogStorageCount() <= 0) {
			return;
		}
		autoClear(tableName, timeClo, ServerExtConfigBean.getInstance().getH2DbLogStorageCount(), time -> {
			Entity entity = Entity.create(tableName);
			entity.set(timeClo, "< " + time);
			int count = 0;
			try {
				count = Db.use().setWrapper((Character) null).del(entity);
			} catch (SQLException e) {
				DefaultSystemLog.getLog().error("清理数据异常", e);
			}
			DefaultSystemLog.getLog().info("{} 清理了 {}条数据", tableName, count);
		});
	}


	public static void autoClear(String tableName, String timeClo, int maxCount, Consumer<Long> consumer) {
		autoClear(tableName, timeClo, maxCount, null, consumer);
	}

	/**
	 * 自动清理数据接口
	 *
	 * @param tableName 表名
	 * @param timeClo   时间字段
	 * @param maxCount  最大数量
	 * @param consumer  查询出超过范围的时间回调
	 */
	public static void autoClear(String tableName, String timeClo, int maxCount, Consumer<Entity> whereCon, Consumer<Long> consumer) {
		if (maxCount <= 0) {
			return;
		}
		ThreadUtil.execute(() -> {
			Entity entity = Entity.create(tableName);
			if (whereCon != null) {
				// 条件
				whereCon.accept(entity);
			}
			Page page = new Page(maxCount, 1);
			page.addOrder(new Order(timeClo, Direction.DESC));
			PageResult<Entity> pageResult;
			try {
				pageResult = Db.use().setWrapper((Character) null).page(entity, page);
				if (pageResult.isEmpty()) {
					return;
				}
				Entity entity1 = pageResult.get(0);
				long time = Convert.toLong(entity1.get(timeClo.toUpperCase()), 0L);
				if (time <= 0) {
					return;
				}
				consumer.accept(time);
			} catch (SQLException e) {
				DefaultSystemLog.getLog().error("数据库查询异常", e);
			}
		});
	}
}
