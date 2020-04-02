package io.jpom.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 基础枚举接口
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public interface BaseEnum {
    /**
     * 缓存
     */
    Map<Class<? extends BaseEnum>, Map<Integer, BaseEnum>> CLASS_MAP_MAP = new HashMap<>();
    Map<Class<? extends Enum>, JSONArray> JSON_ARRAY_MAP = new HashMap<>();

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
        return CLASS_MAP_MAP.computeIfAbsent(t, aClass -> {
            Map<Integer, BaseEnum> map1 = new HashMap<>(20);
            try {
                Method method = t.getMethod("values");
                BaseEnum[] baseEnums = (BaseEnum[]) method.invoke(null);
                for (BaseEnum item : baseEnums) {
                    map1.put(item.getCode(), item);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
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
    static <T extends BaseEnum> T getEnum(Class<? extends BaseEnum> t, int code) {
        Map<Integer, BaseEnum> map = getMap(t);
        if (map == null) {
            return null;
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
    static String getDescByCode(Class<? extends BaseEnum> t, int code) {
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
    static JSONObject toJSONObject(Enum baseEnum) throws InvocationTargetException, IllegalAccessException {
        Class itemCls = baseEnum.getClass();
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
    static JSONArray toJSONArray(Class<? extends Enum> cls) {
        if (!cls.isEnum()) {
            throw new IllegalArgumentException("不是枚举");
        }
        JSONArray getJsonArray = JSON_ARRAY_MAP.computeIfAbsent(cls, aClass -> {
            JSONArray jsonArray = new JSONArray();
            try {
                Method values = aClass.getMethod("values");
                Object[] objects = (Object[]) values.invoke(null);
                for (Object item : objects) {
                    JSONObject jsonObject = toJSONObject((Enum) item);
                    jsonArray.add(jsonObject);
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            }
            return jsonArray;
        });
        Objects.requireNonNull(getJsonArray);
        return (JSONArray) getJsonArray.clone();
    }
}
