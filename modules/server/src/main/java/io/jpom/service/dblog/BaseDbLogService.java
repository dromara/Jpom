package io.jpom.service.dblog;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.PageUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import io.jpom.system.JpomRuntimeException;
import io.jpom.system.db.DbConfig;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * db 日志记录表
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
public abstract class BaseDbLogService<T> {
    /**
     * 表名
     */
    private final String tableName;
    private final Class<T> tClass;
    /**
     * 主键
     */
    private String key;

    String getTableName() {
        return tableName;
    }

    static {
        PageUtil.setFirstPageNo(1);
    }

    public BaseDbLogService(String tableName, Class<T> tClass) {
        this.tableName = tableName;
        this.tClass = tClass;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    /**
     * 插入数据
     *
     * @param t 数据
     */
    public void insert(T t) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
            return;
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(tableName);
            entity.parseBean(t);
            db.insert(entity);
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
        if (!DbConfig.getInstance().isInit()) {
            // ignore
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
        if (entity == null) {
            return null;
        }
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        return BeanUtil.mapToBean(entity, this.tClass, copyOptions);
    }

    /**
     * 根据主键生成
     *
     * @param keyValue 主键值
     * @return 影响行数
     */
    public int delByKey(String keyValue) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
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
     * 分页查询
     *
     * @param where 条件
     * @param page  分页
     * @return 结果
     */
    public PageResult<T> listPage(Entity where, Page page) {
        if (!DbConfig.getInstance().isInit()) {
            // ignore
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
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        List<T> list = new ArrayList<>();
        pageResult.forEach(entity1 -> {
            T v1 = BeanUtil.mapToBean(entity1, this.tClass, copyOptions);
            list.add(v1);
        });
        PageResult<T> pageResult1 = new PageResult<>(pageResult.getPage(), pageResult.getPageSize(), pageResult.getTotal());
        pageResult1.addAll(list);
        return pageResult1;
    }
}
