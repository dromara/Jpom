package io.jpom.system;

import cn.hutool.cache.impl.CacheObj;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.CronUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.pool.ThreadPoolService;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.commander.AbstractSystemCommander;
import io.jpom.model.system.ProcessModel;
import io.jpom.util.CronUtils;
import io.jpom.util.SocketSessionUtil;

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
    /**
     * 最近30分钟监控数据
     */
    private static final TimedCache<String, JSONObject> MONITOR_CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(30), new LinkedHashMap<>());

    /**
     * 是否开启首页监听（自动刷新）
     */
    private static final AtomicBoolean WATCH = new AtomicBoolean(false);

//    /**
//     * 添加top 命令监听
//     *
//     * @param session 会话
//     */
//    public static void addMonitor(Session session) {
//        SESSIONS.add(session);
//        addCron();
//    }

//    /**
//     * 移除top 命令监控
//     *
//     * @param session 会话
//     */
//    public static void removeMonitor(Session session) {
//        SESSIONS.remove(session);
//        close();
//    }

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

//    /**
//     * 创建定时执行top
//     */
//    private static void addCron() {
//        if (WATCH.get()) {
//            return;
//        }
//        CronUtil.remove(CRON_ID);
//        CronUtil.schedule(CRON_ID, "0/30 * * * * ?", () -> {
//            //发送监控信息
//            try {
//                JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
//                if (topInfo != null) {
//                    DateTime date = DateUtil.date();
//                    String time = DateUtil.formatTime(date);
//                    topInfo.put("time", time);
//                    topInfo.put("monitorTime", date.getTime());
//                    MONITOR_CACHE.put(time, topInfo);
//                    send(topInfo.toString());
//                }
//            } catch (Exception e) {
//                DefaultSystemLog.ERROR().error(e.getMessage(), e);
//            }
//            //发送首页进程列表信息
//            sendProcessList();
//        });
//        CronUtils.start();
//        WATCH.set(true);
//    }

//    /**
//     * 发送首页进程列表信息
//     */
//    private static void sendProcessList() {
//        executorService.execute(() -> {
//            List<ProcessModel> array = AbstractSystemCommander.getInstance().getProcessList();
//            if (array != null) {
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("processList", array);
//                send(jsonObject.toJSONString());
//            }
//        });
//    }


//    /**
//     * 同步发送消息
//     *
//     * @param content 内容
//     */
//    private static void send(String content) {
//        synchronized (TopManager.class) {
//            String htmlContent = content.replaceAll("\n", "<br/>");
//            htmlContent = htmlContent.replaceAll(" ", "&nbsp;&nbsp;");
//            Iterator<Session> iterator = SESSIONS.iterator();
//            while (iterator.hasNext()) {
//                Session session = iterator.next();
//                try {
//                    SocketSessionUtil.send(session, htmlContent);
//                } catch (IOException e) {
//                    DefaultSystemLog.ERROR().error("消息失败", e);
//                    try {
//                        session.close();
//                        iterator.remove();
//                    } catch (IOException ignored) {
//                    }
//                }
//            }
//            close();
//        }
//    }

//    /**
//     * 关闭top监听
//     */
//    private static void close() {
//        // 如果没有队列就停止监听
//        if (SESSIONS.isEmpty()) {
//            //
//            CronUtil.remove(CRON_ID);
//            WATCH.set(false);
//        }
//    }
}
