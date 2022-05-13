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
package io.jpom.system;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.commander.AbstractSystemCommander;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * top命令管理，保证整个服务器只获取一个top命令
 *
 * @author jiangzeyin
 * @since 2018/10/2
 */
public class TopManager {
    /**
     * 最近30分钟监控数据
     */
    private static final TimedCache<String, JSONObject> MONITOR_CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(30), new LinkedHashMap<>());

    public static Iterator<CacheObj<String, JSONObject>> get() {
        JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
        if (topInfo != null) {
            DateTime date = DateUtil.date();
            String time = DateUtil.formatTime(date);
            topInfo.put("time", time);
            topInfo.put("monitorTime", date.getTime());
            MONITOR_CACHE.put(time, topInfo);
        }
        return MONITOR_CACHE.cacheObjIterator();
    }
}
