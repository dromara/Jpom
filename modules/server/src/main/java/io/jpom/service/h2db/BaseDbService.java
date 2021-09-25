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
package io.jpom.service.h2db;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
		// check id
		String id = info.getId();
		Assert.hasText(id, "不能执行：error");
		// def modify time
		info.setModifyTimeMillis(ObjectUtil.defaultIfNull(info.getModifyTimeMillis(), SystemClock.now()));
		// remove create time
		info.setCreateTimeMillis(null);
		//
		Entity entity = this.dataBeanToEntity(info);
		//
		entity.remove(StrUtil.format("`{}`", Const.ID_STR));
		//
		Entity where = new Entity();
		where.set(Const.ID_STR, id);
		return super.update(entity, where);
	}

	@Override
	public int update(T t) {
		return this.updateById(t);
	}
}
