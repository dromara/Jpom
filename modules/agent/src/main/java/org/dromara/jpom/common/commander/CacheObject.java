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
        if (cacheObject.isExpired()) {
            map.remove(key);
            return null;
        }
        return cacheObject.value;
    }
}
