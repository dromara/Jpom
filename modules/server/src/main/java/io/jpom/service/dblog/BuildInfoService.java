package io.jpom.service.dblog;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.Entity;
import io.jpom.common.Const;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
		String sql = "select `GROUP` from " + getTableName() + " where 1=1 group by `GROUP`";
		List<Entity> list = super.query(sql);
		// 筛选字段
		return list.stream()
				.filter(entity -> StringUtils.hasLength(String.valueOf(entity.get(Const.GROUP_STR))))
				.flatMap(entity -> Stream.of(String.valueOf(entity.get(Const.GROUP_STR))))
				.distinct()
				.collect(Collectors.toList());
	}


	@Override
	public int updateById(BuildInfoModel info) {
		// check id
		Assert.hasText(info.getId(), "不能执行：error");
		// def modify time
		info.setModifyTimeMillis(ObjectUtil.defaultIfNull(info.getModifyTimeMillis(), SystemClock.now()));
		// remove create time
		info.setCreateTimeMillis(null);

		Entity entity = new Entity(getTableName());
		entity.parseBean(info, false, true);

		/**
		 * reset group field name
		 * group is key words
		 */
		if (null != info.getGroup()) {
			entity.remove(Const.GROUP_STR);
			entity.put(Const.GROUP_COLUMN_STR, info.getGroup());
		}

		Entity where = new Entity();
		where.set(Const.ID_STR, info.getId());
		return super.update(entity, where);
	}
}
