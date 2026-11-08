/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.db;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Editor;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.sql.Condition;
import cn.hutool.extra.spring.SpringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.dialect.DialectUtil;
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
    @Getter
    protected final String tableName;
    protected final Class<T> tClass;
    protected final DbExtConfig.Mode dbMode;

    protected DbExtConfig extConfig;

    @SuppressWarnings("unchecked")
    public BaseDbCommonService() {
        this.tClass = (Class<T>) TypeUtil.getTypeArgument(this.getClass());
        TableName annotation = tClass.getAnnotation(TableName.class);
        Assert.notNull(annotation, I18nMessageUtil.get("i18n.configure_table_name.f6fd"));
        this.extConfig = SpringUtil.getBean(DbExtConfig.class);
        this.tableName = parseRealTableName(annotation);
        this.dbMode = extConfig.getMode();
    }

    public String parseRealTableName(TableName annotation) {
        return StorageServiceFactory.getInstance().parseRealTableName(annotation);
    }

    public String getDataDesc() {
        TableName annotation = tClass.getAnnotation(TableName.class);
        Assert.notNull(annotation, I18nMessageUtil.get("i18n.configure_table_name.f6fd"));
        return I18nMessageUtil.get(annotation.nameKey());
    }

    protected DataSource getDataSource() {
        DSFactory dsFactory = StorageServiceFactory.getInstance().get().getDsFactory();
        return dsFactory.getDataSource();
    }

    /**
     * 插入数据
     *
     * @param t 数据
     */
    protected final int insertDb(T t) {
        Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
        Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
        Map<String, Object> beanToMap = BeanUtil.beanToMap(data, new LinkedHashMap<>(), true, DialectUtil::wrapField);
        entity.putAll(beanToMap);
        return entity;
    }

    /**
     * 实体转查询条件 entity
     * <p>
     * 与 {@link #dataBeanToEntity(Object)} 的区别：查询条件的值统一使用占位符
     * {@link Condition}（{@code new Condition(field, "=", value)}）绑定，
     * 避免 Hutool 把字符串值解析为 SQL 条件表达式（例如 {@code is not null}、{@code = null}），
     * 从而防止请求参数被当作 SQL 片段注入（见 issue #121 同类问题）。
     * <p>
     * 注意：该方法仅用于查询/删除条件，不能用于 INSERT/UPDATE 需要写入的数据行。
     *
     * @param data 实体对象
     * @return 查询条件 entity
     */
    public Entity dataBeanToQueryEntity(T data) {
        return dataBeanToQueryEntity(tableName, data, DialectUtil::wrapField);
    }

    /**
     * 实体转查询条件 entity（可指定表名与字段名处理，便于单元测试）
     *
     * @param tableName 表名
     * @param data      实体对象
     * @param keyEditor 字段名处理（通常是数据库字段包裹函数）
     * @return 查询条件 entity
     */
    public static Entity dataBeanToQueryEntity(String tableName, Object data, Editor<String> keyEditor) {
        Entity entity = new Entity(tableName);
        Map<String, Object> beanToMap = BeanUtil.beanToMap(data, new LinkedHashMap<>(), true, keyEditor);
        for (Map.Entry<String, Object> entry : beanToMap.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Condition || value instanceof Collection) {
                // 调用方显式传入的条件对象或集合（IN 查询）保持原样
                entity.set(field, value);
            } else {
                // 强制使用占位符运算符，值不会再被 Condition#parseValue 解析为 SQL 片段
                entity.set(field, new Condition(field, "=", value));
            }
        }
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
        Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
        if (where.isEmpty()) {
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.update_condition_not_found.0870"));
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
        // 使用 Condition 绑定主键值，避免 Hutool 将 "is not null" 一类的字符串解析为 SQL 条件表达式
        // （主键值来自请求参数时属于注入点，见 issue #121 同类问题）
        where.set(ID_STR, new Condition(ID_STR, "=", keyValue));
        Entity entity;
        try {
            Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
            Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.no_deletion_condition.19d0"));
        }
        try {
            Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
        Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
            return Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode)).count(sql, params);
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
        Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
        Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
        Db db = Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode));
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
            Assert.state(pageResultDto.getTotal() <= 0, I18nMessageUtil.get("i18n.pagination_error.6759"));
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
            return Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode)).query(sql, params);
        } catch (Exception e) {
            throw warpException(e);
        }
    }

    public Number queryNumber(String sql, Object... params) {
        try {
            return Db.use(this.getDataSource()).queryNumber(sql, params);
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
            return Db.use(this.getDataSource(), DialectUtil.getDialectByMode(dbMode)).execute(sql, params);
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
        return StorageServiceFactory.getInstance().get().warpException(e);
    }
}
