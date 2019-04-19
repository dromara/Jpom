package cn.keepbx.jpom.system;

import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.pool.ThreadPoolService;
import cn.keepbx.jpom.common.commander.AbstractSystemCommander;
import cn.keepbx.jpom.model.system.ProcessModel;
import cn.keepbx.jpom.util.SocketSessionUtil;
import com.alibaba.fastjson.JSONObject;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
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
        CronUtil.schedule(CRON_ID, "0/5 * * * * ?", () -> {
            //发送监控信息
            try {
                JSONObject topInfo = AbstractSystemCommander.getInstance().getAllMonitor();
                if (topInfo != null) {
                    send(topInfo.toString());
                }
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error(e.getMessage(), e);
            }
            //发送首页进程列表信息
            sendProcessList();
        });
        Scheduler scheduler = CronUtil.getScheduler();
        if (!scheduler.isStarted()) {
            CronUtil.start();
        }
        WATCH.set(true);
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
            Iterator<Session> iterator = SESSIONS.iterator();
            while (iterator.hasNext()) {
                Session session = iterator.next();
                content = content.replaceAll("\n", "<br/>");
                content = content.replaceAll(" ", "&nbsp;&nbsp;");
                try {
                    SocketSessionUtil.send(session, content);
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
