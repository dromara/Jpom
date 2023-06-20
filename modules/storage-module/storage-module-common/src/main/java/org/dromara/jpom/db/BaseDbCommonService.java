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
package org.dromara.jpom.db;

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
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.sql.Condition;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.util.Assert;

import javax.sql.DataSource;
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
     * String const
     */
    public static final String ID_STR = "id";

    /**
     * 表名
     */
    protected final String tableName;
    protected final Class<T> tClass;


    @SuppressWarnings("unchecked")
    public BaseDbCommonService() {
        this.tClass = (Class<T>) TypeUtil.getTypeArgument(this.getClass());
        TableName annotation = tClass.getAnnotation(TableName.class);
        Assert.notNull(annotation, "请配置 table Name");
        this.tableName = annotation.value();
    }

    public String getTableName() {
        return tableName;
    }

    private DataSource getDataSource() {
        DSFactory dsFactory = StorageServiceFactory.get().getDsFactory();
        return dsFactory.getDataSource();
    }

    /**
     * 插入数据
     *
     * @param t 数据
     */
    protected final int insertDb(T t) {
        Db db = Db.use(this.getDataSource());
        try {
            Entity entity = this.dataBeanToEntity(t);
            return db.insert(entity);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * 插入数据
     *
     * @param t 数据
     */
    protected final void insertDb(Collection<T> t) {
        if (CollUtil.isEmpty(t)) {
            return;
        }
        Db db = Db.use(this.getDataSource());
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
     * 修改数据
     *
     * @param entity 要修改的数据
     * @param where  条件
     * @return 影响行数
     */
    protected final int updateDb(Entity entity, Entity where) {
        Db db = Db.use(this.getDataSource());
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
     * @param fill     是否执行填充逻辑
     * @param consumer 参数回调
     * @return 数据
     */
    public final T getByKey(String keyValue, boolean fill, Consumer<Entity> consumer) {
        if (StrUtil.isEmpty(keyValue)) {
            return null;
        }
        Entity where = new Entity(tableName);
        where.set(ID_STR, keyValue);
        Entity entity;
        try {
            Db db = Db.use(this.getDataSource());
            if (consumer != null) {
                consumer.accept(where);
            }
            entity = db.get(where);
        } catch (Exception e) {
            throw warpException(e);
        }
        return this.entityToBean(entity, fill);
    }

    /**
     * 根据主键查询实体
     *
     * @param keyValue 主键值
     * @param fill     是否执行填充逻辑
     * @param consumer 参数回调
     * @return 数据
     */
    public final List<T> getByKey(Collection<String> keyValue, boolean fill, Consumer<Entity> consumer) {
        if (CollUtil.isEmpty(keyValue)) {
            return null;
        }
        Entity where = new Entity(tableName);
        where.set(ID_STR, keyValue);
        List<Entity> entities;
        try {
            Db db = Db.use(this.getDataSource());
            if (consumer != null) {
                consumer.accept(where);
            }
            entities = db.find(where);
        } catch (Exception e) {
            throw warpException(e);
        }
        return this.entityToBeanList(entities, fill);
    }

    /**
     * 根据条件删除
     *
     * @param where 条件
     * @return 影响行数
     */
    public final int del(Entity where) {
        where.setTableName(tableName);
        if (where.isEmpty()) {
            throw new JpomRuntimeException("没有删除条件");
        }
        try {
            Db db = Db.use(this.getDataSource());
            return db.del(where);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * 查询记录条数
     *
     * @param where 条件
     * @return count
     */
    public final long count(Entity where) {
        where.setTableName(getTableName());
        Db db = Db.use(this.getDataSource());
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
    public final long count(String sql, Object... params) {
        try {
            return Db.use(this.getDataSource()).count(sql, params);
        } catch (Exception e) {
            throw warpException(e);
        }
    }


    /**
     * 查询列表
     *
     * @param where 条件
     * @return List
     */
    public final List<Entity> queryList(Entity where) {
        where.setTableName(getTableName());
        Db db = Db.use(this.getDataSource());
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
    public final List<T> findByCondition(Condition... wheres) {
        Db db = Db.use(this.getDataSource());
        try {
            List<Entity> entities = db.findBy(getTableName(), wheres);
            return this.entityToBeanList(entities);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    /**
     * entity 转 实体
     *
     * @param entity Entity
     * @param fill   是否填充
     * @return data
     */
    protected T entityToBean(Entity entity, boolean fill) {
        if (entity == null) {
            return null;
        }
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        T toBean = BeanUtil.toBean(entity, this.tClass, copyOptions);
        if (fill) {
            this.fillSelectResult(toBean);
        }
        return toBean;
    }

    public List<T> entityToBeanList(List<Entity> entitys) {
        return this.entityToBeanList(entitys, true);
    }

    public List<T> entityToBeanList(List<Entity> entitys, boolean fill) {
        if (entitys == null) {
            return null;
        }
        return entitys.stream()
            .map((entity -> this.entityToBean(entity, fill)))
            .collect(Collectors.toList());
    }

    /**
     * 分页查询
     *
     * @param where 条件
     * @param page  分页
     * @param fill  是否填充
     * @return 结果
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final PageResultDto<T> listPageDb(Entity where, Page page, boolean fill) {
        where.setTableName(getTableName());
        PageResult<Entity> pageResult;
        Db db = Db.use(this.getDataSource());
        try {
            pageResult = db.page(where, page);
        } catch (Exception e) {
            throw warpException(e);
        }
        //
        List<T> list = this.entityToBeanList(pageResult, fill);
        PageResultDto<T> pageResultDto = new PageResultDto(pageResult);
        pageResultDto.setResult(list);
        if (pageResultDto.isEmpty() && pageResultDto.getPage() > 1) {
            Assert.state(pageResultDto.getTotal() <= 0, "筛选的分页有问题,当前页码查询不到任何数据");
        }
        return pageResultDto;
    }


    /**
     * sql 查询
     *
     * @param sql    sql 语句
     * @param params 参数
     * @return list
     */
    public final List<Entity> query(String sql, Object... params) {
        try {
            return Db.use(this.getDataSource()).query(sql, params);
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
    public final int execute(String sql, Object... params) {
        try {
            return Db.use(this.getDataSource()).execute(sql, params);
        } catch (Exception e) {
            throw warpException(e);
        }
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
    private JpomRuntimeException warpException(Exception e) {
        return StorageServiceFactory.get().warpException(e);
    }
}
