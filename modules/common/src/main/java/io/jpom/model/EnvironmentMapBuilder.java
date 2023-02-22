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
package io.jpom.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;

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

    public EnvironmentMapBuilder putObject(Map<String, Object> map) {
        Optional.ofNullable(map).ifPresent(stringMap -> {
            for (Map.Entry<String, Object> entry : stringMap.entrySet()) {
                put(entry.getKey(), StrUtil.toString(entry.getValue()));
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
            map.put(entry.getKey(), entryValue.value);
        }
        Optional.ofNullable(appendMap).ifPresent(objectMap -> {
            for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
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

    @AllArgsConstructor
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
