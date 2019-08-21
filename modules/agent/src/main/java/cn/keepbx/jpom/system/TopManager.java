package cn.keepbx.jpom.system;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.pool.ThreadPoolService;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import cn.keepbx.util.CronUtils;
import cn.keepbx.util.SocketSessionUtil;
import com.alibaba.fastjson.JSONObject;

import javax.websocket.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * top命令管理，保证整个服务器只获取一个top命令
 *
 * @author jiangzeyin
 * @date 2018/10/2
 */
public class TopManager {

    private static final Set<Session> SESSIONS = new HashSet<>();
    private static final String CRON_ID = "topMonitor";
    private static ExecutorService executorService = ThreadPoolService.newCachedThreadPool(TopManager.class);
    private static final TimedCache<String, JSONObject> MONITOR_CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(12), new LinkedHashMap<>());

    /**
     * 是否开启首页监听（自动刷新）
     */
    private static final AtomicBoolean WATCH = new AtomicBoolean(false);

    /**
     * 添加top 命令监听
     *
     * @param session 会话
     */
    public static void addMonitor(Session session) {
        SESSIONS.add(session);
        addCron();
    }

    /**
     * 移除top 命令监控
     *
     * @param session 会话
     */
    public static void removeMonitor(Session session) {
        SESSIONS.remove(session);
        close();
    }

    /**
     * 创建定时执行top
     */
    private static void addCron() {
        if (WATCH.get()) {
            return;
        }
        CronUtil.remove(CRON_ID);
        CronUtil.schedule(CRON_ID, "0/30 * * * * ?", () -> {
            //发送监控信息
            try {
                JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
                if (topInfo != null) {
                    String time = DateUtil.formatTime(DateUtil.date());
                    topInfo.put("time", time);
                    MONITOR_CACHE.put(time, topInfo);
                    send(topInfo.toString());
                }
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error(e.getMessage(), e);
            }
            //发送首页进程列表信息
            sendProcessList();
        });
        CronUtils.start();
        WATCH.set(true);
    }

    /**
     * 缓存的监控信息
     *
     * @return 监控信息
     */
    public static JSONObject getTopMonitor() {
        Iterator<CacheObj<String, JSONObject>> cacheObjIterator = MONITOR_CACHE.cacheObjIterator();
        String lastTime = "";
        List<JSONObject> array = new ArrayList<>();
        List<String> scale = new ArrayList<>();
        while (cacheObjIterator.hasNext()) {
            CacheObj<String, JSONObject> cacheObj = cacheObjIterator.next();
            String key = cacheObj.getKey();
            if (StrUtil.isNotEmpty(lastTime)) {
                if (!key.equals(getLastTime(lastTime))) {
                    array.clear();
                    scale.clear();
                }
            }
            lastTime = key;
            scale.add(key);
            JSONObject value = cacheObj.getValue();
            array.add(value);
        }
        int count = 24;
        if (array.size() > count) {
            array = array.subList(array.size() - count - 1, array.size() - 1);
        }
        while (scale.size() < count) {
            if (scale.size() == 0) {
                DateTime date = DateUtil.date();
                int second = date.second();
                if (second <= 30 && second > 0) {
                    second = 30;
                } else if (second > 30) {
                    second = 0;
                    date = date.offset(DateField.MINUTE, 1);
                }
                String format = DateUtil.format(date, "HH:mm");
                String secondStr = ":" + second;
                if (second < 10) {
                    secondStr = ":0" + second;
                }
                scale.add(format + secondStr);
            }
            String time = scale.get(scale.size() - 1);
            String newTime = getLastTime(time);
            scale.add(newTime);
        }
        JSONObject object = new JSONObject();
        object.put("scale", scale);
        object.put("series", array);
        return object;
    }

    private static String getLastTime(String time) {
        DateTime dateTime = DateUtil.parseTime(time);
        DateTime newTime = dateTime.offsetNew(DateField.SECOND, 30);
        return DateUtil.formatTime(newTime);
    }

    /**
     * 发送首页进程列表信息
     */
    private static void sendProcessList() {
        executorService.execute(() -> {
            List<ProcessModel> array = AbstractSystemCommander.getInstance().getProcessList();
            if (array != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("processList", array);
                send(jsonObject.toJSONString());
            }
        });
    }


    /**
     * 同步发送消息
     *
     * @param content 内容
     */
    private static void send(String content) {
        synchronized (TopManager.class) {
            String htmlContent = content.replaceAll("\n", "<br/>");
            htmlContent = htmlContent.replaceAll(" ", "&nbsp;&nbsp;");
            Iterator<Session> iterator = SESSIONS.iterator();
            while (iterator.hasNext()) {
                Session session = iterator.next();
                try {
                    SocketSessionUtil.send(session, htmlContent);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error("消息失败", e);
                    try {
                        session.close();
                        iterator.remove();
                    } catch (IOException ignored) {
                    }
                }
            }
            close();
        }
    }

    /**
     * 关闭top监听
     */
    private static void close() {
        // 如果没有队列就停止监听
        if (SESSIONS.isEmpty()) {
            //
            CronUtil.remove(CRON_ID);
            WATCH.set(false);
        }
    }
}
