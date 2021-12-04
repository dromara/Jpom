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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.*;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.BaseDbModel;
import io.jpom.model.BaseUserModifyDbModel;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
		this.fillInsert(t);
		super.insert(t);
	}

	/**
	 * 不填充 插入
	 *
	 * @param t 数据
	 */
	public void insertNotFill(T t) {
		// def create time
		t.setCreateTimeMillis(ObjectUtil.defaultIfNull(t.getCreateTimeMillis(), SystemClock.now()));
		t.setId(ObjectUtil.defaultIfNull(t.getId(), IdUtil.fastSimpleUUID()));
		super.insert(t);
	}

	@Override
	public void insert(Collection<T> t) {
		// def create time
		t.forEach(this::fillInsert);
		super.insert(t);
	}

	/**
	 * 插入数据填充
	 *
	 * @param t 数据
	 */
	protected void fillInsert(T t) {
		// def create time
		t.setCreateTimeMillis(ObjectUtil.defaultIfNull(t.getCreateTimeMillis(), SystemClock.now()));
		t.setId(ObjectUtil.defaultIfNull(t.getId(), IdUtil.fastSimpleUUID()));
		if (t instanceof BaseUserModifyDbModel) {
			// 获取数据修改人
			BaseUserModifyDbModel modifyDbModel = (BaseUserModifyDbModel) t;
			UserModel userModel = BaseServerController.getUserModel();
			if (userModel != null) {
				modifyDbModel.setModifyUser(ObjectUtil.defaultIfNull(modifyDbModel.getModifyUser(), userModel.getId()));
			}
		}
		if (t instanceof BaseWorkspaceModel) {
			// 数据都将绑定一个默认的工作空间
			BaseWorkspaceModel baseWorkspaceModel = (BaseWorkspaceModel) t;
			baseWorkspaceModel.setWorkspaceId(ObjectUtil.defaultIfNull(baseWorkspaceModel.getWorkspaceId(), WorkspaceModel.DEFAULT_ID));
		}
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
		// fill modify user
		if (info instanceof BaseUserModifyDbModel) {
			BaseUserModifyDbModel modifyDbModel = (BaseUserModifyDbModel) info;
			UserModel userModel = BaseServerController.getUserModel();
			if (userModel != null) {
				modifyDbModel.setModifyUser(ObjectUtil.defaultIfNull(modifyDbModel.getModifyUser(), userModel.getId()));
			}
		}
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


	public List<T> list() {
		return super.listByBean(ReflectUtil.newInstance(this.tClass));
	}

	public long count() {
		return super.count(Entity.create());
	}


	/**
	 * 通用的分页查询, 使用该方法查询，数据库表字段不能保护 "page", "limit", "order_field", "order", "total"
	 * <p>
	 * page=1&limit=10&order=ascend&order_field=name
	 *
	 * @param request 请求对象
	 * @return page
	 */
	public PageResultDto<T> listPage(HttpServletRequest request) {
		Map<String, String> paramMap = ServletUtil.getParamMap(request);
		return this.listPage(paramMap);
	}

	/**
	 * 通用的分页查询, 使用该方法查询，数据库表字段不能保护 "page", "limit", "order_field", "order", "total"
	 * <p>
	 * page=1&limit=10&order=ascend&order_field=name
	 *
	 * @param paramMap 请求参数
	 * @return page
	 */
	public PageResultDto<T> listPage(Map<String, String> paramMap) {
		int page = Convert.toInt(paramMap.get("page"), 1);
		int limit = Convert.toInt(paramMap.get("limit"), 10);
		Assert.state(page > 0, "page value error");
		Assert.state(limit > 0 && limit < 200, "limit value error");
		String orderField = paramMap.get("order_field");
		String order = paramMap.get("order");
		// 移除 默认字段
		MapUtil.removeAny(paramMap, "page", "limit", "order_field", "order", "total");
		//
		Page pageReq = new Page(page, limit);
		Entity where = Entity.create();
		// 查询条件
		for (Map.Entry<String, String> stringStringEntry : paramMap.entrySet()) {
			String key = stringStringEntry.getKey();
			String value = stringStringEntry.getValue();
			key = StrUtil.removeAll(key, "%");
			if (StrUtil.startWith(stringStringEntry.getKey(), "%") && StrUtil.endWith(stringStringEntry.getKey(), "%")) {
				where.setIgnoreNull(StrUtil.format("`{}`", key), StrUtil.format(" like '%{}%'", value));
			} else if (StrUtil.endWith(stringStringEntry.getKey(), "%")) {
				where.setIgnoreNull(StrUtil.format("`{}`", key), StrUtil.format(" like '{}%'", value));
			} else if (StrUtil.startWith(stringStringEntry.getKey(), "%")) {
				where.setIgnoreNull(StrUtil.format("`{}`", key), StrUtil.format(" like '%{}'", value));
			} else {
				where.setIgnoreNull(StrUtil.format("`{}`", key), value);
			}
		}
		// 排序
		if (StrUtil.isNotEmpty(orderField)) {
			orderField = StrUtil.removeAll(orderField, "%");
			pageReq.addOrder(new Order(orderField, StrUtil.equalsIgnoreCase(order, "ascend") ? Direction.ASC : Direction.DESC));
		}
		return this.listPage(where, pageReq);
	}

	@Override
	public PageResultDto<T> listPage(Entity where, Page page) {
		if (ArrayUtil.isEmpty(page.getOrders())) {
			page.addOrder(new Order("createTimeMillis", Direction.DESC));
		}
		return super.listPage(where, page);
	}

	/**
	 * 多个 id 查询数据
	 *
	 * @param ids ids
	 * @return list
	 */
	public List<T> listById(Collection<String> ids) {
		Entity entity = Entity.create();
		entity.set(Const.ID_STR, ids);
		List<Entity> entities = super.queryList(entity);
		return this.entityToBeanList(entities);
	}
}
