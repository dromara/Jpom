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
package org.dromara.jpom.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.RuntimeUtil;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 线程同步器 工具类
 *
 * @author bwcx_jzy
 * @since 2023/3/18
 */
public final class SyncFinisherUtil {

    private static final Map<String, StrictSyncFinisher> SYNC_FINISHER_MAP = new SafeConcurrentHashMap<>();

    /**
     * 任务列表
     *
     * @return 任务列表
     */
    public static Set<String> keys() {
        return SYNC_FINISHER_MAP.keySet();
    }

    /**
     * 创建线程同步器
     *
     * @param core          线程核心数
     * @param queueCapacity 任务队列数
     * @return 线程同步器
     */
    private static StrictSyncFinisher create(int core, int queueCapacity) {
        int threadSize = Math.min(core, RuntimeUtil.getProcessorCount());
        return new StrictSyncFinisher(threadSize, queueCapacity);
    }

    /**
     * 创建线程同步器
     * 核心任务数 为 cpu 核心数
     *
     * @param queueCapacity 任务队列数
     * @param name          任务名
     * @return 线程同步器
     */
    public static StrictSyncFinisher create(String name, int queueCapacity) {
        int threadSize = Math.min(RuntimeUtil.getProcessorCount(), queueCapacity);
        StrictSyncFinisher strictSyncFinisher = new StrictSyncFinisher(threadSize, queueCapacity);
        put(name, strictSyncFinisher);
        return strictSyncFinisher;
    }

    /**
     * 添加任务
     *
     * @param name         任务名
     * @param syncFinisher 同步器
     */
    public static void put(String name, StrictSyncFinisher syncFinisher) {
        Assert.state(!SYNC_FINISHER_MAP.containsKey(name), "任务已经存在啦");
        SYNC_FINISHER_MAP.put(name, syncFinisher);
    }

    /**
     * 取消 任务
     *
     * @param name 任务名
     */
    public static boolean cancel(String name) {
        StrictSyncFinisher syncFinisher = SYNC_FINISHER_MAP.remove(name);
        Optional.ofNullable(syncFinisher).ifPresent(StrictSyncFinisher::stopNow);
        return syncFinisher != null;
    }

    /**
     * 关闭任务
     *
     * @param name 任务名
     */
    public static void close(String name) {
        IoUtil.close(SYNC_FINISHER_MAP.remove(name));
    }
}
