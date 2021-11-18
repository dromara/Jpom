package io.jpom.service.h2db;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.ds.DSFactory;
import cn.jiangzeyin.common.DefaultSystemLog;
import org.h2.tools.RunScript;
import org.h2.tools.Shell;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * H2 数据库备份与还原 service
 * @author Hotstrip
 * @since 2021-11-17
 */
@Service
public class H2BackupService {

	/**
	 * 备份 SQL
	 * @param url jdbc url
	 * @param user user
	 * @param password password
	 * @param backupSqlPath backup SQL file path, absolute path
	 * @param tableNameList backup table name list, if need backup all table, use null
	 */
	public void backupSql(String url, String user, String password,
						  String backupSqlPath, List<String> tableNameList) throws SQLException {
		// 加载数据源
		DataSource dataSource = DSFactory.get();
		Assert.notNull(dataSource, "Backup sql error...H2 DataSource not null");
		Connection connection = dataSource.getConnection();

		// 备份 SQL
		String sql = StrUtil.format("SCRIPT DROP to '{}'", backupSqlPath);
		// 判断是否部分部分表
		if (!CollectionUtils.isEmpty(tableNameList)) {
			String tableNames = StrUtil.join(",", tableNameList.toArray());
			sql = StrUtil.format("{} TABLE {}", sql, tableNames);
		}

		DefaultSystemLog.getLog().info("backup SQL is: {}", sql);

		// 执行 SQL 备份脚本
		Shell shell = new Shell();

		/**
		 * url 表示 h2 数据库的 jdbc url
		 * user 表示登录的用户名
		 * password 表示登录密码
		 * driver 是 jdbc 驱动
		 * sql 是备份的 sql 语句
		 * - 案例：script drop to ${fileName1} table ${tableName1},${tableName2}...
		 * - script drop to 表示备份数据库，drop 表示建表之前会先删除表
		 * - ${fileName1} 表示备份之后的文件名
		 * - table 表示需要备份的表名称，后面跟多个表名，用英文逗号分割
		 */
		String[] params = new String[] {
				"-url", url,
				"-user", user,
				"-password", password,
				"-driver", "org.h2.Driver",
				"-sql", sql
		};
		shell.runTool(connection, params);
	}

	/**
	 * 还原备份 SQL
	 * @param backupSqlPath backup SQL file path, absolute path
	 * @throws SQLException
	 * @throws FileNotFoundException
	 */
	public void restoreBackupSql(String backupSqlPath) throws SQLException, FileNotFoundException {
		// 读取数据库备份文件
		FileReader fileReader = new FileReader(backupSqlPath);

		// 加载数据源
		DataSource dataSource = DSFactory.get();
		Assert.notNull(dataSource, "Restore Backup sql error...H2 DataSource not null");
		Connection connection = dataSource.getConnection();

		// 执行还原
		RunScript.execute(connection, fileReader);
	}
}
