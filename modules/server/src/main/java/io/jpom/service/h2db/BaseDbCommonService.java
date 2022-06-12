/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
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
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Condition;
import cn.hutool.db.sql.Order;
import io.jpom.model.PageResultDto;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.db.DbConfig;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import org.h2.jdbc.JdbcSQLNonTransientException;
import org.h2.mvstore.MVStoreException;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 数据库基础操作 通用 service
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@Slf4j
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

    public String getTableName() {
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
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return;
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = this.dataBeanToEntity(t);
            db.insert(entity);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * 插入数据
     *
     * @param t 数据
     */
    public void insert(Collection<T> t) {
        if (CollUtil.isEmpty(t)) {
            return;
        }
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return;
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            List<Entity> entities = t.stream().map(this::dataBeanToEntity).collect(Collectors.toList());
            db.insert(entities);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * 实体转 entity
     *
     * @param data 实体对象
     * @return entity
     */
    public Entity dataBeanToEntity(T data) {
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
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return 0;
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        entity.setTableName(tableName);
        try {
            return db.insert(entity);
        } catch (Exception e) {
            throw warpException(e);
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
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
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
        } catch (Exception e) {
            throw warpException(e);
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
        return this.getByKey(keyValue, fill, null);
    }

    /**
     * 根据主键查询实体
     *
     * @param keyValue 主键值
     * @param fill     是否执行填充逻辑
     * @param consumer 参数回调
     * @return 数据
     */
    public T getByKey(String keyValue, boolean fill, Consumer<Entity> consumer) {
        if (StrUtil.isEmpty(keyValue)) {
            return null;
        }
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return null;
        }
        Entity where = new Entity(tableName);
        where.set(key, keyValue);
        Entity entity;
        try {
            Db db = Db.use();
            db.setWrapper((Character) null);
            if (consumer != null) {
                consumer.accept(where);
            }
            entity = db.get(where);
        } catch (Exception e) {
            throw warpException(e);
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
    protected <R> R entityToBean(Entity entity, Class<R> rClass) {
        if (entity == null) {
            return null;
        }
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        return BeanUtil.toBean(entity, rClass, copyOptions);
    }

    /**
     * entity 转 实体
     *
     * @param entity Entity
     * @return data
     */
    public T entityToBean(Entity entity) {
        if (entity == null) {
            return null;
        }
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        T toBean = BeanUtil.toBean(entity, this.tClass, copyOptions);
        this.fillSelectResult(toBean);
        return toBean;
    }

    /**
     * 根据主键生成
     *
     * @param keyValue 主键值
     * @return 影响行数
     */
    public int delByKey(String keyValue) {
        return this.delByKey(keyValue, null);
    }

    /**
     * 根据主键生成
     *
     * @param keyValue 主键值
     * @param consumer 回调
     * @return 影响行数
     */
    public int delByKey(Object keyValue, Consumer<Entity> consumer) {
        //		if (ObjectUtil.isEmpty(keyValue)) {
        //			return 0;
        //		}
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return 0;
        }
        Entity where = new Entity(tableName);
        if (keyValue != null) {
            where.set(key, keyValue);
        }
        if (consumer != null) {
            consumer.accept(where);
        }
        Assert.state(where.size() > 0, "没有添加任何参数:-1");
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
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return 0;
        }
        where.setTableName(tableName);
        if (where.isEmpty()) {
            throw new JpomRuntimeException("没有删除条件");
        }
        try {
            Db db = Db.use();
            db.setWrapper((Character) null);
            return db.del(where);
        } catch (Exception e) {
            throw warpException(e);
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
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return 0;
        }
        where.setTableName(getTableName());
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            return db.count(where);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * 查询记录条数
     *
     * @param sql sql
     * @return count
     */
    public long count(String sql, Object... params) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return 0;
        }
        try {
            return Db.use().count(sql, params);
        } catch (Exception e) {
            throw warpException(e);
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
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return null;
        }
        where.setTableName(getTableName());
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            return db.find(where);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * 查询列表
     *
     * @param wheres 条件
     * @return List
     */
    public List<T> findByCondition(Condition... wheres) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return null;
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            List<Entity> entities = db.findBy(getTableName(), wheres);
            return this.entityToBeanList(entities);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * 查询列表
     *
     * @param data   数据
     * @param count  查询数量
     * @param orders 排序
     * @return List
     */
    public List<T> queryList(T data, int count, Order... orders) {
        Entity where = this.dataBeanToEntity(data);
        Page page = new Page(1, count);
        page.addOrder(orders);
        PageResultDto<T> tPageResultDto = this.listPage(where, page);
        return tPageResultDto.getResult();
    }

    /**
     * 分页查询
     *
     * @param where 条件
     * @param page  分页
     * @return 结果
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public PageResultDto<T> listPage(Entity where, Page page) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return PageResultDto.EMPTY;
        }
        where.setTableName(getTableName());
        PageResult<Entity> pageResult;
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            pageResult = db.page(where, page);
        } catch (Exception e) {
            throw warpException(e);
        }
        //
        List<T> list = pageResult.stream().map(entity -> {
            T entityToBean = this.entityToBean(entity, this.tClass);
            this.fillSelectResult(entityToBean);
            return entityToBean;
        }).collect(Collectors.toList());
        PageResultDto<T> pageResultDto = new PageResultDto(pageResult);
        pageResultDto.setResult(list);
        if (pageResultDto.isEmpty() && pageResultDto.getPage() > 1) {
            Assert.state(pageResultDto.getTotal() <= 0, "筛选的分页有问题,当前页码查询不到任何数据");
        }
        return pageResultDto;
    }

    /**
     * 分页查询
     *
     * @param where 条件
     * @param page  分页
     * @return 结果
     */
    public List<T> listPageOnlyResult(Entity where, Page page) {
        PageResultDto<T> pageResultDto = this.listPage(where, page);
        return pageResultDto.getResult();
    }

    /**
     * sql 查询
     *
     * @param sql    sql 语句
     * @param params 参数
     * @return list
     */
    public List<Entity> query(String sql, Object... params) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return null;
        }
        try {
            return Db.use().query(sql, params);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * sql 执行
     *
     * @param sql    sql 语句
     * @param params 参数
     * @return list
     */
    public int execute(String sql, Object... params) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            log.error("The database is not initialized, this execution will be ignored:{},{}", this.tClass, this.getClass());
            return 0;
        }
        try {
            return Db.use().execute(sql, params);
        } catch (Exception e) {
            throw warpException(e);
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
        return this.entityToBeanList(query);
        //		if (query != null) {
        //			return query.stream().map((entity -> {
        //				T entityToBean = this.entityToBean(entity, this.tClass);
        //				this.fillSelectResult(entityToBean);
        //				return entityToBean;
        //			})).collect(Collectors.toList());
        //		}
        //		return null;
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
        return this.entityToBeanList(entitys);
    }

    public List<T> entityToBeanList(List<Entity> entitys) {
        if (entitys == null) {
            return null;
        }
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

    /**
     * 包裹异常
     *
     * @param e 异常
     */
    protected JpomRuntimeException warpException(Exception e) {
        String message = e.getMessage();
        if (e instanceof MVStoreException || ExceptionUtil.isCausedBy(e, MVStoreException.class)) {
            if (StrUtil.containsIgnoreCase(message, "The write format 1 is smaller than the supported format 2")) {
                log.warn(message);
                String tip = "升级数据库流程：" + StrUtil.LF;
                tip += StrUtil.TAB + "1. 导出低版本数据 【启动程序参数里面添加 --backup-h2】" + StrUtil.LF;
                tip += StrUtil.TAB + "2. 将导出的低版本数据( sql 文件) 导入到新版本中【启动程序参数里面添加 --replace-import-h2-sql=/xxxx.sql (路径需要替换为第一步控制台输出的 sql 文件保存路径)】";
                return new JpomRuntimeException("数据库版本不兼容,需要处理跨版本升级。" + StrUtil.LF + tip + StrUtil.LF, -1);
            }
        }
        if (e instanceof JdbcSQLNonTransientException || ExceptionUtil.isCausedBy(e, JdbcSQLNonTransientException.class)) {
            return new JpomRuntimeException("数据库异常,可能数据库文件已经损坏(可能丢失部分数据),需要重新初始化。可以尝试在启动参数里面添加 --recover:h2db 来自动恢复,：" + message, e);
        }
        if (e instanceof JdbcSQLNonTransientConnectionException) {
            return new JpomRuntimeException("数据库异常,可能因为服务器资源不足（内存、硬盘）等原因造成数据异常关闭。需要手动重启服务端来恢复，：" + message, e);
        }
        return new JpomRuntimeException("数据库异常", e);
    }
}
