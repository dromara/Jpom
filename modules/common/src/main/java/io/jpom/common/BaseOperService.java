package io.jpom.common;

import cn.hutool.core.util.ClassUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.BaseModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 标准操作Service
 *
 * @author jiangzeyin
 * @date 2019/3/14
 */
public abstract class BaseOperService<T extends BaseModel> extends BaseDataService {

    private final String fileName;
    private final Class<?> typeArgument;

    public BaseOperService(String fileName) {
        this.fileName = fileName;
        this.typeArgument = ClassUtil.getTypeArgument(this.getClass());
    }

    /**
     * 获取所有数据
     *
     * @return list
     */
    public List<T> list() {
        return (List<T>) list(typeArgument);
    }

    public <E> List<E> list(Class<E> cls) {
        JSONObject jsonObject = getJSONObject();
        if (jsonObject == null) {
            return new ArrayList<>();
        }
        JSONArray jsonArray = formatToArray(jsonObject);
        return jsonArray.toJavaList(cls);
    }

    public JSONObject getJSONObject() {
        Objects.requireNonNull(fileName, "没有配置fileName");
        return getJSONObject(fileName);
    }

    /**
     * 工具id 获取 实体
     *
     * @param id 数据id
     * @return T
     */
    public T getItem(String id) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        return (T) getJsonObjectById(fileName, id, typeArgument);
    }


    /**
     * 添加实体
     *
     * @param t 实体
     */
    public void addItem(T t) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        saveJson(fileName, t);
    }

    /**
     * 删除实体
     *
     * @param id 数据id
     */
    public void deleteItem(String id) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        deleteJson(fileName, id);
    }

    /**
     * 修改实体
     *
     * @param t 实体
     */
    public void updateItem(T t) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        updateJson(fileName, t);
    }

    private JSONArray formatToArray(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new JSONArray();
        }
        Set<String> setKey = jsonObject.keySet();
        JSONArray jsonArray = new JSONArray();
        for (String key : setKey) {
            jsonArray.add(jsonObject.getJSONObject(key));
        }
        return jsonArray;
    }
}
