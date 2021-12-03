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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.db.DbConfig;

import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库基础操作 通用 service
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
public abstract class BaseDbCommonService<T> {

	static {
		// 配置页码是从 1 开始
		PageUtil.setFirstPageNo(1);
	}

	/**
	 * 表名
	 */
	protected final String tableName;
	protected final Class<T> tClass;
	/**
	 * 主键
	 */
	protected final String key;

	public BaseDbCommonService(String tableName, String key, Class<T> tClass) {
		this.tableName = this.covetTableName(tableName, tClass);
		this.tClass = tClass;
		this.key = key;
	}

	@SuppressWarnings("unchecked")
	public BaseDbCommonService(String tableName, String key) {
		this.tClass = (Class<T>) TypeUtil.getTypeArgument(this.getClass());
		this.tableName = this.covetTableName(tableName, this.tClass);
		this.key = key;
	}

	/**
	 * 转换表面
	 *
	 * @param tableName 表面
	 * @param tClass    类
	 * @return 转换后的表名
	 */
	protected String covetTableName(String tableName, Class<T> tClass) {
		return tableName;
	}

	protected String getTableName() {
		return tableName;
	}

	protected String getKey() {
		return key;
	}

	/**
	 * 插入数据
	 *
	 * @param t 数据
	 */
	public void insert(T t) {
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return;
		}
		Db db = Db.use();
		db.setWrapper((Character) null);
		try {
			Entity entity = this.dataBeanToEntity(t);
			db.insert(entity);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}

	/**
	 * 插入数据
	 *
	 * @param t 数据
	 */
	public void insert(Collection<T> t) {
		if (!DbConfig.getInstance().isInit() || CollUtil.isEmpty(t)) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return;
		}
		Db db = Db.use();
		db.setWrapper((Character) null);
		try {
			List<Entity> entities = t.stream().map(this::dataBeanToEntity).collect(Collectors.toList());
			db.insert(entities);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}

	/**
	 * 实体转 entity
	 *
	 * @param data 实体对象
	 * @return entity
	 */
	protected Entity dataBeanToEntity(T data) {
		Entity entity = new Entity(tableName);
		// 转换为 map
		Map<String, Object> beanToMap = BeanUtil.beanToMap(data, new LinkedHashMap<>(), true, s -> StrUtil.format("`{}`", s));
		entity.putAll(beanToMap);
		return entity;
	}

	/**
	 * 插入数据
	 *
	 * @param entity 要修改的数据
	 * @return 影响行数
	 */
	public int insert(Entity entity) {
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return 0;
		}
		Db db = Db.use();
		db.setWrapper((Character) null);
		entity.setTableName(tableName);
		try {
			return db.insert(entity);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}

	/**
	 * 修改数据，需要自行实现
	 *
	 * @param t 数据
	 * @return 影响行数
	 */
	public int update(T t) {
		return 0;
	}

	/**
	 * 修改数据
	 *
	 * @param entity 要修改的数据
	 * @param where  条件
	 * @return 影响行数
	 */
	public int update(Entity entity, Entity where) {
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return 0;
		}
		Db db = Db.use();
		db.setWrapper((Character) null);
		if (where.isEmpty()) {
			throw new JpomRuntimeException("没有更新条件");
		}
		entity.setTableName(tableName);
		where.setTableName(tableName);
		try {
			return db.update(entity, where);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}

	/**
	 * 根据主键查询实体
	 *
	 * @param keyValue 主键值
	 * @return 数据
	 */
	public T getByKey(String keyValue) {
		return this.getByKey(keyValue, true);
	}

	/**
	 * 根据主键查询实体
	 *
	 * @param keyValue 主键值
	 * @return 数据
	 */
	public T getByKey(String keyValue, boolean fill) {
		if (StrUtil.isEmpty(keyValue)) {
			return null;
		}
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return null;
		}
		Entity where = new Entity(tableName);
		where.set(key, keyValue);
		Db db = Db.use();
		db.setWrapper((Character) null);
		Entity entity;
		try {
			entity = db.get(where);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
		T entityToBean = this.entityToBean(entity, this.tClass);
		if (fill) {
			this.fillSelectResult(entityToBean);
		}
		return entityToBean;
	}

	/**
	 * entity 转 实体
	 *
	 * @param entity Entity
	 * @param rClass 实体类
	 * @param <R>    乏型
	 * @return data
	 */
	private <R> R entityToBean(Entity entity, Class<R> rClass) {
		if (entity == null) {
			return null;
		}
		CopyOptions copyOptions = new CopyOptions();
		copyOptions.setIgnoreError(true);
		copyOptions.setIgnoreCase(true);
		return BeanUtil.toBean(entity, rClass, copyOptions);
	}

	/**
	 * 根据主键生成
	 *
	 * @param keyValue 主键值
	 * @return 影响行数
	 */
	public int delByKey(String keyValue) {
		if (StrUtil.isEmpty(keyValue)) {
			return 0;
		}
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return 0;
		}
		Entity where = new Entity(tableName);
		where.set(key, keyValue);
		return del(where);
	}

	/**
	 * 根据条件删除
	 *
	 * @param where 条件
	 * @return 影响行数
	 */
	public int del(Entity where) {
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return 0;
		}
		where.setTableName(tableName);
		if (where.isEmpty()) {
			throw new JpomRuntimeException("没有删除条件");
		}
		Db db = Db.use();
		db.setWrapper((Character) null);
		try {
			return db.del(where);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}

	/**
	 * 判断是否存在
	 *
	 * @param data 实体
	 * @return true 存在
	 */
	public boolean exists(T data) {
		Entity entity = this.dataBeanToEntity(data);
		return this.exists(entity);
	}

	/**
	 * 判断是否存在
	 *
	 * @param where 条件
	 * @return true 存在
	 */
	public boolean exists(Entity where) {
		long count = this.count(where);
		return count > 0;
	}

	/**
	 * 查询记录条数
	 *
	 * @param where 条件
	 * @return count
	 */
	public long count(Entity where) {
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return 0;
		}
		where.setTableName(getTableName());
		Db db = Db.use();
		db.setWrapper((Character) null);
		try {
			return db.count(where);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}

	/**
	 * 查询一个
	 *
	 * @param where 条件
	 * @return Entity
	 */
	public Entity query(Entity where) {
		List<Entity> entities = this.queryList(where);
		return CollUtil.getFirst(entities);
	}

	/**
	 * 查询列表
	 *
	 * @param where 条件
	 * @return List
	 */
	public List<Entity> queryList(Entity where) {
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return null;
		}
		where.setTableName(getTableName());
		Db db = Db.use();
		db.setWrapper((Character) null);
		try {
			return db.find(where);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}

	/**
	 * 分页查询
	 *
	 * @param where 条件
	 * @param page  分页
	 * @return 结果
	 */
	public PageResult<T> listPage(Entity where, Page page) {
		if (!DbConfig.getInstance().isInit()) {
			// ignore
			DefaultSystemLog.getLog().error("The database is not initialized, this execution will be ignored");
			return new PageResult<>(page.getPageNumber(), page.getPageSize(), 0);
		}
		where.setTableName(getTableName());
		PageResult<Entity> pageResult;
		Db db = Db.use();
		db.setWrapper((Character) null);
		try {
			pageResult = db.page(where, page);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
		//
		List<T> list = pageResult.stream().map(entity -> {
			T entityToBean = this.entityToBean(entity, this.tClass);
			this.fillSelectResult(entityToBean);
			return entityToBean;
		}).collect(Collectors.toList());
		PageResult<T> pageResult1 = new PageResult<>(pageResult.getPage(), pageResult.getPageSize(), pageResult.getTotal());
		pageResult1.addAll(list);
		return pageResult1;
	}

	/**
	 * sql 查询
	 *
	 * @param sql    sql 语句
	 * @param params 参数
	 * @return list
	 */
	public List<Entity> query(String sql, Object... params) {
		try {
			return Db.use().query(sql, params);
		} catch (SQLException e) {
			throw new JpomRuntimeException("数据库异常", e);
		}
	}


	/**
	 * sql 查询 list
	 *
	 * @param sql    sql 语句
	 * @param params 参数
	 * @return list
	 */
	public List<T> queryList(String sql, Object... params) {
		List<Entity> query = this.query(sql, params);
		if (query == null) {
			return null;
		}
		return query.stream().map((entity -> {
			T entityToBean = this.entityToBean(entity, this.tClass);
			this.fillSelectResult(entityToBean);
			return entityToBean;
		})).collect(Collectors.toList());
	}

	/**
	 * 查询实体对象
	 *
	 * @param data 实体
	 * @return data
	 */
	public List<T> listByBean(T data) {
		Entity where = this.dataBeanToEntity(data);
		List<Entity> entitys = this.queryList(where);
		return entitys.stream().map((entity -> {
			T entityToBean = this.entityToBean(entity, this.tClass);
			this.fillSelectResult(entityToBean);
			return entityToBean;
		})).collect(Collectors.toList());
	}

	/**
	 * 查询实体对象
	 *
	 * @param data 实体
	 * @return data
	 */
	public T queryByBean(T data) {
		Entity where = this.dataBeanToEntity(data);
		Entity entity = this.query(where);
		T entityToBean = this.entityToBean(entity, this.tClass);
		this.fillSelectResult(entityToBean);
		return entityToBean;
	}

	/**
	 * 查询结果 填充
	 *
	 * @param data 数据
	 */
	protected void fillSelectResult(T data) {
	}
}
