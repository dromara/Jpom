package io.jpom.service.h2db;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.db.Entity;
import io.jpom.common.Const;
import io.jpom.model.BaseDbModel;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * 数据库操作 通用 serve
 *
 * @author bwcx_jzy
 * @since 2021/8/13
 */
public abstract class BaseDbService<T extends BaseDbModel> extends BaseDbCommonService<T> {

	public BaseDbService() {
		super(null);
		setKey(Const.ID_STR);
	}

	@Override
	protected String covetTableName(String tableName, Class<T> tClass) {
		TableName annotation = tClass.getAnnotation(TableName.class);
		Assert.notNull(annotation, "请配置 table Name");
		return annotation.value();
	}

	@Override
	public void insert(T t) {
		// def create time
		t.setCreateTimeMillis(ObjectUtil.defaultIfNull(t.getCreateTimeMillis(), SystemClock.now()));
		super.insert(t);
	}


	@Override
	public void insert(Collection<T> t) {
		// def create time
		t.forEach(t1 -> t1.setCreateTimeMillis(ObjectUtil.defaultIfNull(t1.getCreateTimeMillis(), SystemClock.now())));
		super.insert(t);
	}

	/**
	 * update by id with data
	 *
	 * @param info data
	 */
	public void updateById(T info) {
		// def modify time
		info.setModifyTimeMillis(ObjectUtil.defaultIfNull(info.getModifyTimeMillis(), SystemClock.now()));
		Entity entity = new Entity(getTableName());
		entity.parseBean(info, false, true);

		Entity where = new Entity();
		where.set(Const.ID_STR, info.getId());
		super.update(entity, where);
	}


	/**
	 * delete by id
	 *
	 * @param id 主键
	 */
	public void deleteById(String id) {
		Entity where = new Entity(getTableName());
		where.set(Const.ID_STR, id);
		del(where);
	}
}
