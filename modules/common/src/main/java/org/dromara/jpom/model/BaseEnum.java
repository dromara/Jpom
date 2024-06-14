/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import cn.hutool.log.StaticLog;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 基础枚举接口
 *
 * @author bwcx_jzy
 * @since 2019/4/19
 */
public interface BaseEnum {
    /**
     * 缓存
     */
    class Cache {
        private static final Map<Class<? extends BaseEnum>, Map<Integer, BaseEnum>> CLASS_MAP_MAP = new HashMap<>();
        @SuppressWarnings("rawtypes")
        private static final Map<Class<? extends Enum>, JSONArray> JSON_ARRAY_MAP = new HashMap<>();
    }

    /**
     * 枚举的code
     *
     * @return int
     */
    int getCode();

    /**
     * 枚举的描述
     *
     * @return 描述
     */
    String getDesc();

    /**
     * 将枚举转换为map
     *
     * @param t class
     * @return mao
     */
    static Map<Integer, BaseEnum> getMap(Class<? extends BaseEnum> t) {
        return Cache.CLASS_MAP_MAP.computeIfAbsent(t, aClass -> {
            Map<Integer, BaseEnum> map1 = new HashMap<>(20);
            try {
                Method method = t.getMethod("values");
                BaseEnum[] baseEnums = (BaseEnum[]) method.invoke(null);
                for (BaseEnum item : baseEnums) {
                    map1.put(item.getCode(), item);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                StaticLog.error("enum error", e);
                return null;
            }
            return map1;
        });
    }

    /**
     * 根据枚举获取枚举对象
     *
     * @param t    枚举类型
     * @param code code
     * @param <T>  泛型
     * @return 对应的枚举
     */
    static <T extends BaseEnum> T getEnum(Class<? extends BaseEnum> t, Integer code) {
        return getEnum(t, code, null);
    }

    /**
     * 根据枚举获取枚举对象
     *
     * @param t    枚举类型
     * @param code code
     * @param def  默认值
     * @param <T>  泛型
     * @return 对应的枚举
     */
    @SuppressWarnings("unchecked")
    static <T extends BaseEnum> T getEnum(Class<? extends BaseEnum> t, Integer code, T def) {
        if (code == null) {
            return def;
        }
        Map<Integer, BaseEnum> map = getMap(t);
        if (map == null) {
            return def;
        }
        return (T) map.get(code);
    }

    /**
     * 根据 code 获取描述
     *
     * @param t    class
     * @param code code
     * @return desc
     */
    static String getDescByCode(Class<? extends BaseEnum> t, Integer code) {
        BaseEnum baseEnums = getEnum(t, code);
        if (baseEnums == null) {
            return null;
        }
        return baseEnums.getDesc();
    }

    /**
     * 获取 json
     *
     * @param baseEnum 枚举对象
     * @return json
     * @throws InvocationTargetException e
     * @throws IllegalAccessException    e
     */
    @SuppressWarnings("rawtypes")
    static JSONObject toJSONObject(Enum baseEnum) throws InvocationTargetException, IllegalAccessException {
        Class<?> itemCls = baseEnum.getClass();
        Method[] methods = itemCls.getMethods();
        JSONObject jsonObject = new JSONObject();
        for (Method method : methods) {
            String name = method.getName();
            if (!name.startsWith("get")) {
                continue;
            }
            name = name.substring(3);
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
            try {
                itemCls.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                continue;
            }
            jsonObject.put(name, method.invoke(baseEnum));
        }
        return jsonObject;
    }

    /**
     * 将枚举转化为数组
     * 包括里面所有属性
     *
     * @param cls cls
     * @return array
     */
    @SuppressWarnings("rawtypes")
    static JSONArray toJSONArray(Class<? extends Enum<?>> cls) {
        if (!cls.isEnum()) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.not_an_enumeration.8244"));
        }
        JSONArray getJsonArray = Cache.JSON_ARRAY_MAP.computeIfAbsent(cls, aClass -> {
            JSONArray jsonArray = new JSONArray();
            try {
                Method values = aClass.getMethod("values");
                Object[] objects = (Object[]) values.invoke(null);
                for (Object item : objects) {
                    JSONObject jsonObject = toJSONObject((Enum) item);
                    jsonArray.add(jsonObject);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                StaticLog.error("enum error", e);
                return null;
            }
            return jsonArray;
        });
        Objects.requireNonNull(getJsonArray);
        return (JSONArray) getJsonArray.clone();
    }
}
