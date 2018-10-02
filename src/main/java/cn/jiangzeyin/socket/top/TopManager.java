package cn.jiangzeyin.socket.top;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.service.manage.CommandService;
import cn.jiangzeyin.socket.SocketSession;

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by jiangzeyin on 2018/10/2.
 */
public class TopManager {

    private static final Set<Session> SESSIONS = new HashSet<>();
    private static final String CRON_ID = "topMonitor";
    private static CommandService commandService;
    private static WatchMonitor watchMonitor;
    private static boolean watch = false;

    public static void addMonitor(Session session) {
        SESSIONS.add(session);
        addCron();
    }

    public static void removeMonitor(Session session) {
        SESSIONS.remove(session);
        close(null);
    }

    public static String getTopFile() {
        String savePath = "~/top.txt";
        try {
            savePath = new File(commandService.getDataPath(), "top.txt").getPath();
        } catch (IOException ignored) {

        }
        return savePath;
    }

    private static void addCron() {
        if (watch) {
            return;
        }
        if (commandService == null) {
            commandService = SpringUtil.getBean(CommandService.class);
        }
        CronUtil.remove(CRON_ID);
        CronUtil.setMatchSecond(true);
        CronUtil.schedule(CRON_ID, "0/10 * * * * ?", () -> {
            String result = commandService.execCommand(CommandService.CommandOp.top, null, null);
            System.out.println(result);
        });
        CronUtil.restart();
        watchMonitor = WatchUtil.create(getTopFile());
        watchMonitor.watch(new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {

            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                send();
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                close("文件被删除");
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                close("监听事件丢失");
            }
        });
        watch = true;
    }

    private static void send() {
        synchronized (TopManager.class) {
            System.out.println("发送");
            Iterator<Session> iterator = SESSIONS.iterator();
            while (iterator.hasNext()) {
                Session session = iterator.next();
                String content = FileUtil.readString(getTopFile(), CharsetUtil.GBK);
                content = content.replaceAll("\r\n", "<br/>").replaceAll("\n", "<br/>");
                try {
                    SocketSession.send(session, content);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error("消息失败", e);
                    try {
                        session.close();
                        iterator.remove();
                    } catch (IOException ignored) {
                    }
                }
            }
            close(null);
        }
    }

    private static void close(String msg) {
        // 如果没有队列就停止监听
        int size = SESSIONS.size();
        if (size > 0) {
            return;
        }
        if (msg != null) {
            Iterator<Session> iterator = SESSIONS.iterator();
            while (iterator.hasNext()) {
                Session session = iterator.next();
                try {
                    SocketSession.send(session, msg);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error("消息失败", e);
                }
                try {
                    session.close();
                    iterator.remove();
                } catch (IOException ignored) {
                }
            }
        }
        if (watchMonitor != null) {
            watchMonitor.close();
        }
        CronUtil.remove(CRON_ID);
        watch = false;
    }
}
