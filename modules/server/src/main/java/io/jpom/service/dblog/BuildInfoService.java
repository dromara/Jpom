package io.jpom.service.dblog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import io.jpom.build.BuildManage;
import io.jpom.common.BaseOperService;
import io.jpom.common.Const;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 构建 service 新版本，数据从数据库里面加载
 * @author Hotstrip
 * @date 2021-08-10
 **/
@Service
public class BuildInfoService extends BaseDbLogService<BuildInfoModel> {

	/**
	 * constructor, init table name and primary key
	 */
    public BuildInfoService() {
		super(BuildInfoModel.TABLE_NAME, BuildInfoModel.class);
		setKey(Const.ID_STR);
    }

	/**
	 * load date group by group name
	 * @return
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
