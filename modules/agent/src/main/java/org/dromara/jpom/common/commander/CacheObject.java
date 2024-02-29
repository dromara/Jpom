/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander;

import cn.hutool.core.date.SystemClock;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2023/4/6
 */
public class CacheObject<T> {

    private final T value;

    private final Long enterTime;

    public CacheObject(T value) {
        this.value = value;
        this.enterTime = SystemClock.now();
    }

    private boolean isExpired() {
        return (System.currentTimeMillis() - this.enterTime > TimeUnit.MINUTES.toMillis(10));
    }

    /**
     * 添加到缓存对象中
     *
     * @param map   map
     * @param key   缓存的 key
     * @param value 缓存的 value
     * @param <K>   缓存的 key
     * @param <V>   缓存的 value
     */
    public static <K, V> void put(Map<K, CacheObject<V>> map, K key, V value) {
        map.put(key, new CacheObject<>(value));
        int size = map.size();
        if (size > 100) {
            // 清空过期的数据
            Iterator<Map.Entry<K, CacheObject<V>>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, CacheObject<V>> next = iterator.next();
                CacheObject<V> nextValue = next.getValue();
                if (nextValue.isExpired()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 获取缓存中的值
     *
     * @param map 缓存 map
     * @param key 缓存的 key
     * @param <K> 缓存的 key
     * @return value
     */
    public static <K, V> V get(Map<K, CacheObject<V>> map, K key) {
        CacheObject<V> cacheObject = map.get(key);
        if (cacheObject == null || cacheObject.isExpired()) {
            map.remove(key);
            return null;
        }
        return cacheObject.value;
    }
}
