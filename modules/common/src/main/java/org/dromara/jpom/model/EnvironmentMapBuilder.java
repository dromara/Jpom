/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 环境变量管理
 *
 * @author bwcx_jzy
 * @since 2023/2/11
 */
public class EnvironmentMapBuilder {

    private final Map<String, Item> map;

    public EnvironmentMapBuilder(int initialCapacity) {
        map = new LinkedHashMap<>(initialCapacity);
    }

    public static EnvironmentMapBuilder builder(Map<String, Item> map) {
        EnvironmentMapBuilder environmentMapBuilder = new EnvironmentMapBuilder(map.size() + 10);
        environmentMapBuilder.put(map);
        return environmentMapBuilder;
    }

    public EnvironmentMapBuilder put(String name, String value) {
        map.put(name, new Item(value, false));
        return this;
    }

    public EnvironmentMapBuilder put(Map<String, Item> map) {
        if (map != null) {
            this.map.putAll(map);
        }
        return this;
    }

    public EnvironmentMapBuilder putStr(Map<String, String> map) {
        Optional.ofNullable(map).ifPresent(stringMap -> {
            for (Map.Entry<String, String> entry : stringMap.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        });
        return this;
    }

    public EnvironmentMapBuilder putObjectArray(Object... parametersEnv) {
        for (int i = 0; i < parametersEnv.length; i += 2) {
            this.put(StrUtil.toString(parametersEnv[i]), StrUtil.toString(parametersEnv[i + 1]));
        }
        return this;
    }

    public Map<String, String> environment() {
        return this.environment(null);
    }

    public Map<String, String> environment(Map<String, Object> appendMap) {
        Map<String, String> map = new LinkedHashMap<>(this.map.size());
        for (Map.Entry<String, Item> entry : this.map.entrySet()) {
            Item entryValue = entry.getValue();
            if (entryValue.value == null || entry.getKey() == null) {
                // 值不能为 null
                continue;
            }
            map.put(entry.getKey(), entryValue.value);
        }
        Optional.ofNullable(appendMap).ifPresent(objectMap -> {
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                Object value = entry.getValue();
                if (value == null || entry.getKey() == null) {
                    continue;
                }
                map.put(entry.getKey(), StrUtil.toStringOrNull(value));
            }
        });
        return map;
    }

    public void eachStr(Consumer<String> consumer) {
        this.eachStr(consumer, null);
    }

    /**
     * 输出环境变量信息
     *
     * @param consumer  回调
     * @param appendMap 附加的环境变量
     */
    public void eachStr(Consumer<String> consumer, Map<String, Object> appendMap) {
        int allSize = CollUtil.size(this.map) + CollUtil.size(appendMap);
        if (allSize <= 0) {
            return;
        }
        consumer.accept("##################################################################################");
        for (Map.Entry<String, Item> entry : map.entrySet()) {
            Item entryValue = entry.getValue();
            String value = entryValue.privacy ? "******" : entryValue.value;
            consumer.accept(entry.getKey() + "=" + value);
        }
        Optional.ofNullable(appendMap).ifPresent(objectMap -> {
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                consumer.accept(entry.getKey() + "=" + StrUtil.toString(entry.getValue()));
            }
        });
        consumer.accept("##################################################################################");
    }

    /**
     * 获取环境变量的执行
     *
     * @param key 变量名
     * @return 值
     */
    public String get(String key) {
        Item item = map.get(key);
        if (item != null) {
            return item.value;
        }
        return null;
    }

    /**
     * 获取环境变量的执行
     *
     * @param key 变量名
     * @return 值
     */
    public boolean getBool(String key, boolean defaultValue) {
        String value = this.get(key);
        if (value == null) {
            return defaultValue;
        }
        return BooleanUtil.toBoolean(value);
    }

    public String toDataJsonStr() {
        return JSONObject.toJSONString(map);
    }

    public JSONObject toDataJson() {
        return JSONObject.from(map);
    }

    @AllArgsConstructor
    @Data
    public static class Item {
        /**
         * 值
         */
        private String value;
        /**
         * 隐私
         */
        private boolean privacy;
    }
}
