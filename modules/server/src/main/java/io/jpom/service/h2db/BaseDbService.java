package io.jpom.service.h2db;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.IdUtil;
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
		super(null, Const.ID_STR);
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
		t.setId(ObjectUtil.defaultIfNull(t.getId(), IdUtil.fastSimpleUUID()));
		super.insert(t);
	}


	@Override
	public void insert(Collection<T> t) {
		// def create time
		t.forEach(t1 -> {
			t1.setCreateTimeMillis(ObjectUtil.defaultIfNull(t1.getCreateTimeMillis(), SystemClock.now()));
			t1.setId(ObjectUtil.defaultIfNull(t1.getId(), IdUtil.fastSimpleUUID()));
		});
		super.insert(t);
	}

	/**
	 * update by id with data
	 *
	 * @param info data
	 * @return 影响的行数
	 */
	public int updateById(T info) {
		// def modify time
		info.setModifyTimeMillis(ObjectUtil.defaultIfNull(info.getModifyTimeMillis(), SystemClock.now()));
		// remove create time
		info.setCreateTimeMillis(null);
		Entity entity = new Entity(getTableName());
		entity.parseBean(info, false, true);

		Entity where = new Entity();
		where.set(Const.ID_STR, info.getId());
		return super.update(entity, where);
	}

	@Override
	public int update(T t) {
		return this.updateById(t);
	}
}
