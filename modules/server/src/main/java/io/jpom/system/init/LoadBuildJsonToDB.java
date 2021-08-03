package io.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.GlobalDSFactory;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.Const;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.vo.BuildModelVo;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Hotstrip
 * @date 2021-08-02
 * Auto import build.json data to DB
 */
public class LoadBuildJsonToDB {
	private LoadBuildJsonToDB() {

	}

	/**
	 * 静态内部类实现单例模式
	 */
	public static class LoadBuildJsonToDBHolder {
		private static LoadBuildJsonToDB INSTANCE = new LoadBuildJsonToDB();
	}

	public static LoadBuildJsonToDB getInstance() {
		return LoadBuildJsonToDBHolder.INSTANCE;
	}

	/**
	 * 把 build.json 文件内容写入到数据库
	 */
	public void doJsonToSql() {
		// 读取 build.json 文件内容
		List<Object> list = readBuildJsonFileToList();
		// 转换成 SQL 执行
		initSql(list);
	}

	/**
	 * exec insert SQL to DB
	 * @param sql
	 */
	private void insertToDB(String sql) {
		DSFactory dsFactory = GlobalDSFactory.get();
		int rows = 0;
		try {
			rows = Db.use(dsFactory.getDataSource()).execute(sql);
		} catch (SQLException e) {
			DefaultSystemLog.getLog().warn("exec SQL: {} failed, caused: {}...message: {}",
					sql, e.getCause(), e.getMessage());
		}
		DefaultSystemLog.getLog().info("exec SQL: {} complete, and affected rows is: {}",
				sql, rows);
	}

	/**
	 * list data to SQL
	 * @param list
	 */
	private void initSql(List<Object> list) {
		// 加载类里面的属性，用反射获取
		final List<String> fieldList = getFieldList();

		// 遍历对象集合
		list.forEach(item -> {
			BuildModelVo buildModelVo = JSON.parseObject(item.toString(), BuildModelVo.class);
			DefaultSystemLog.getLog().info("buildModelVo: {}", JSON.toJSONString(buildModelVo));

			Map<String, Object> paramMap = new HashMap<>();
			// 遍历类的属性集合，通过反射获取属性对应的值
			fieldList.forEach(fieldName -> {
				// 判断类里面是否有这个属性
				if (ReflectUtil.hasField(BuildModelVo.class, fieldName)) {
					final String getMethodName = StrUtil.upperFirstAndAddPre(StrUtil.toCamelCase(fieldName), Const.GET_STR);
					Object filedValue = ReflectUtil.invoke(buildModelVo, getMethodName);

					// 添加到参数对象中
					String sqlFiledName = StrUtil.toUnderlineCase(fieldName).toUpperCase();
					paramMap.put(sqlFiledName, filedValue);
				}
			});

			// 构造 insert SQL 语句
			StringBuffer sqlBuffer = new StringBuffer("insert into {} ( ");
			StringBuffer sqlFieldNameBuffer = new StringBuffer();
			StringBuffer sqlFieldValueBuffer = new StringBuffer();
			for (int i = 0; i < paramMap.size(); i++) {
				sqlFieldNameBuffer.append("`{}`,");
				sqlFieldValueBuffer.append("'{}',");
			}
			sqlBuffer.append(sqlFieldNameBuffer.substring(0, sqlFieldNameBuffer.length() - 1))
					.append(" )")
					.append(" values ( ")
					.append(sqlFieldValueBuffer.substring(0, sqlFieldValueBuffer.length() - 1))
					.append(" )");

			// 构造 SQL 参数
			List<Object> params = new ArrayList<>();
			params.add(RepositoryModel.TABLE_NAME);
			params.addAll(paramMap.keySet());
			params.addAll(paramMap.values());
			String sql = StrUtil.format(sqlBuffer, params.toArray());

			// 插入数据库
			insertToDB(sql);
		});
	}

	/**
	 * read build.json file to list
	 * @return
	 */
	private List<Object> readBuildJsonFileToList() {
		File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.BUILD);
		if (!file.exists()) {
			DefaultSystemLog.getLog().error("there is no build.json file...");
			return null;
		}
		try {
			// 读取 build.json 文件里面的内容，转换成实体对象集合
			JSONObject jsonObject = (JSONObject) JsonFileUtil.readJson(file.getAbsolutePath());
			return jsonObject.keySet().stream()
					.map(key -> jsonObject.get(key))
					.collect(Collectors.toList());
		} catch (FileNotFoundException e) {
			DefaultSystemLog.getLog().error("read build.json file failed...caused: {}...message: {}", e.getCause(), e.getMessage());
		}
		return null;
	}

	/**
	 * 获取类里面的属性，转换成集合返回
	 * @return
	 */
	private List<String> getFieldList() {
		final Field[] fields = ReflectUtil.getFieldsDirectly(RepositoryModel.class, true);
		return Arrays.stream(fields)
				.filter(field -> Modifier.isPrivate(field.getModifiers()))
				.flatMap(field -> Arrays.stream(new String[]{field.getName()}))
				.collect(Collectors.toList());
	}

}
