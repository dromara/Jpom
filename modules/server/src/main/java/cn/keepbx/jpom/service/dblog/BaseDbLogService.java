package cn.keepbx.jpom.service.dblog;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.keepbx.jpom.system.JpomRuntimeException;

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

    private String tableName;
    private Class<T> tClass;

    private String key;

    public String getTableName() {
        return tableName;
    }

    public BaseDbLogService(String tableName, Class<T> tClass) {
        this.tableName = tableName;
        this.tClass = tClass;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    public void insert(T t) {
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

    public int update(T t) {
        return 0;
    }

    public int update(Entity entity, Entity where) {
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

    public T getByKey(String keyValue) throws SQLException {
        Entity where = new Entity(tableName);
        where.set(key, keyValue);
        Db db = Db.use();
        db.setWrapper((Character) null);
        Entity entity = db.get(where);
        if (entity == null) {
            return null;
        }
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        return BeanUtil.mapToBean(entity, this.tClass, copyOptions);
    }


    public int delByKey(String keyValue) throws SQLException {
        Entity where = new Entity(tableName);
        where.set(key, keyValue);
        return del(where);
    }

    public int del(Entity where) throws SQLException {
        where.setTableName(tableName);
        if (where.isEmpty()) {
            throw new JpomRuntimeException("没有删除条件");
        }
        Db db = Db.use();
        db.setWrapper((Character) null);
        return db.del(where);
    }

    public PageResult<T> listPage(Entity where, Page page) {
        where.setTableName(getTableName());
        PageResult<Entity> pageResult = null;
        try {
            pageResult = Db.use().page(where, page);
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
