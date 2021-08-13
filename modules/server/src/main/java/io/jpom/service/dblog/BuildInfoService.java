package io.jpom.service.dblog;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.common.Const;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 构建 service 新版本，数据从数据库里面加载
 *
 * @author Hotstrip
 * @date 2021-08-10
 **/
@Service
public class BuildInfoService extends BaseDbService<BuildInfoModel> {

	/**
	 * load date group by group name
	 *
	 * @return list
	 */
	public List<String> listGroup() {
		List<Entity> list = new ArrayList<>();
		try {
			String sql = "select `GROUP` from " + getTableName() + " where 1=1 group by `GROUP`";
			list = Db.use().query(sql);
		} catch (SQLException e) {
			DefaultSystemLog.getLog().error("load data group by group name failed, cause: {}...message: {}",
					e.getCause(), e.getMessage());
		}
		// 筛选字段
		return list.stream()
				.filter(entity -> StringUtils.hasLength(String.valueOf(entity.get(Const.GROUP_STR))))
				.flatMap(entity -> Stream.of(String.valueOf(entity.get(Const.GROUP_STR))))
				.distinct()
				.collect(Collectors.toList());
	}
}
