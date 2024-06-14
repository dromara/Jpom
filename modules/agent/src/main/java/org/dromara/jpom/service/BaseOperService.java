/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.lock.LockUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.BaseModel;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.JsonFileUtil;
import org.springframework.util.Assert;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * 标准操作Service
 *
 * @author bwcx_jzy
 * @since 2019/3/14
 */
public abstract class BaseOperService<T extends BaseModel> {

    private final String fileName;
    private final Class<T> typeArgument;
    private final Lock lock = LockUtil.createStampLock().asWriteLock();

    public BaseOperService(String fileName) {
        this.fileName = fileName;
        this.typeArgument = (Class<T>) ClassUtil.getTypeArgument(this.getClass());
    }

    /**
     * 获取所有数据
     *
     * @return list
     */
    public List<T> list() {
        return list(typeArgument);
    }

    public int size() {
        List<T> list = this.list();
        return CollUtil.size(list);
    }

    public <E> List<E> list(Class<E> cls) {
        JSONObject jsonObject = getJSONObject();
        if (jsonObject == null) {
            return new ArrayList<>();
        }
        JSONArray jsonArray = JsonFileUtil.formatToArray(jsonObject);
        return jsonArray.toJavaList(cls);
    }

    public JSONObject getJSONObject() {
        Objects.requireNonNull(fileName, I18nMessageUtil.get("i18n.file_name_not_configured.39fa"));
        return getJSONObject(fileName);
    }

    /**
     * 工具id 获取 实体
     *
     * @param id 数据id
     * @return T
     */
    public T getItem(String id) {
        Objects.requireNonNull(fileName, I18nMessageUtil.get("i18n.file_name_not_configured.39fa"));
        return getJsonObjectById(fileName, id, typeArgument);
    }


    /**
     * 添加实体
     *
     * @param t 实体
     */
    public void addItem(T t) {
        Objects.requireNonNull(fileName, I18nMessageUtil.get("i18n.file_name_not_configured.39fa"));
        try {
            lock.lock();
            saveJson(fileName, t);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 删除实体
     *
     * @param id 数据id
     */
    public void deleteItem(String id) {
        Objects.requireNonNull(fileName, I18nMessageUtil.get("i18n.file_name_not_configured.39fa"));
        try {
            lock.lock();
            deleteJson(fileName, id);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 修改实体
     *
     * @param t 实体
     */
    public void updateItem(T t) {
        Objects.requireNonNull(fileName, I18nMessageUtil.get("i18n.file_name_not_configured.39fa"));
        try {
            lock.lock();
            updateJson(fileName, t);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据数据Id 修改
     *
     * @param updateData 实体
     * @param id         数据Id
     */
    public void updateById(T updateData, String id) {
        Objects.requireNonNull(fileName, I18nMessageUtil.get("i18n.file_name_not_configured.39fa"));
        try {
            lock.lock();
            T item = getItem(id);
            Assert.notNull(item, I18nMessageUtil.get("i18n.data_does_not_exist.b201"));
            BeanUtil.copyProperties(updateData, item, CopyOptions.create().ignoreNullValue());
            updateJson(fileName, item);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取数据文件的路径，如果文件不存在，则创建一个
     *
     * @param filename 文件名
     * @return path
     */
    protected String getDataFilePath(String filename) {
        return FileUtil.normalize(JpomApplication.getInstance().getDataPath() + StrUtil.SLASH + filename);
    }

    /**
     * 保存json对象
     *
     * @param filename 文件名
     * @param json     json数据
     */
    protected void saveJson(String filename, BaseModel json) {
        String key = json.getId();
        // 读取文件，如果存在记录，则抛出异常
        JSONObject allData = getJSONObject(filename);
        if (allData != null) {
            // 判断是否存在数据
            if (allData.containsKey(key)) {
                throw new JpomRuntimeException(StrUtil.format(I18nMessageUtil.get("i18n.data_id_already_exists.28b6"), filename, key));
            }
        } else {
            allData = new JSONObject();
        }
        allData.put(key, json.toJson());
        JsonFileUtil.saveJson(getDataFilePath(filename), allData);
    }

    /**
     * 修改json对象
     *
     * @param filename 文件名
     * @param json     json数据
     */
    protected void updateJson(String filename, BaseModel json) {
        String key = json.getId();
        // 读取文件，如果不存在记录，则抛出异常
        JSONObject allData = getJSONObject(filename);
        JSONObject data = allData.getJSONObject(key);

        // 判断是否存在数据
        if (MapUtil.isEmpty(data)) {
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.data_does_not_exist_with_details.d9b5") + key);
        } else {
            allData.put(key, json.toJson());
            JsonFileUtil.saveJson(getDataFilePath(filename), allData);
        }
    }

    /**
     * 删除json对象
     *
     * @param filename 文件
     * @param key      key
     */
    protected void deleteJson(String filename, String key) {
        // 读取文件，如果存在记录，则抛出异常
        JSONObject allData = getJSONObject(filename);
        if (allData == null) {
            return;
        }
        //Assert.notNull(allData, "没有任何数据");
        //JSONObject data = allData.getJSONObject(key);
        allData.remove(key);
        JsonFileUtil.saveJson(getDataFilePath(filename), allData);

    }

    /**
     * 读取整个json文件
     *
     * @param filename 文件名
     * @return json
     */
    protected JSONObject getJSONObject(String filename) {
        try {
            return (JSONObject) JsonFileUtil.readJson(getDataFilePath(filename));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    protected T getJsonObjectById(String file, String id, Class<T> cls) {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        JSONObject jsonObject = getJSONObject(file);
        if (jsonObject == null) {
            return null;
        }
        jsonObject = jsonObject.getJSONObject(id);
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toJavaObject(cls);
    }
}
