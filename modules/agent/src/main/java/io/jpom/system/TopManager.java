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
 * @date 2018/10/2
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
