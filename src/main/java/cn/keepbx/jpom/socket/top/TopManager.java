package cn.keepbx.jpom.socket.top;

import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.service.manage.CommandService;
import cn.keepbx.jpom.socket.SocketSessionUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * top命令管理，保证整个服务器只获取一个top命令
 *
 * @author jiangzeyin
 * @date 2018/10/2
 */
public class TopManager {

    private static final Set<Session> SESSIONS = new HashSet<>();
    private static final String CRON_ID = "topMonitor";
    private static CommandService commandService;
    private static boolean watch = false;

    /**
     * 添加top 命令监听
     *
     * @param session 回话
     */
    public static void addMonitor(Session session) {
        SESSIONS.add(session);
        addCron();
    }

    /**
     * 移除top 命令监控
     *
     * @param session 回话
     */
    public static void removeMonitor(Session session) {
        SESSIONS.remove(session);
        close();
    }

    /**
     * 创建定时执行top
     */
    private static void addCron() {
        if (watch) {
            return;
        }
        if (commandService == null) {
            commandService = SpringUtil.getBean(CommandService.class);
        }
        CronUtil.remove(CRON_ID);
        CronUtil.schedule(CRON_ID, "0/5 * * * * ?", () -> {
            try {
                String result = AbstractCommander.getInstance().execCommand("top -b -n 1");
                String topInfo = getTopInfo(result);
                send(topInfo);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error(e.getMessage(), e);
            }
        });
        Scheduler scheduler = CronUtil.getScheduler();
        if (!scheduler.isStarted()) {
            CronUtil.start();
        }
        watch = true;
    }

    public static String getTopInfo(String content) {
        if (StrUtil.isEmpty(content)) {
            return "top查询失败";
        }
        String[] split = content.split("\n");
        int length = split.length;
        JSONObject jsonObject = new JSONObject();
        if (length >= 2) {
            String cpus = split[2];
            JSONArray cpu = getCpu(cpus);
            jsonObject.put("cpu", cpu);
        }
        if (length >= 3) {
            String mem = split[3];
            JSONArray memory = getMemory(mem);
            jsonObject.put("memory", memory);
        }
        jsonObject.put("top", true);
        return jsonObject.toJSONString();
    }

    /**
     * 获取内存信息
     *
     * @param info 内存信息
     * @return 内存信息
     */
    private static JSONArray getMemory(String info) {
        if (StrUtil.isEmpty(info)) {
            return null;
        }
        int index = info.indexOf(":") + 1;
        String[] split = info.substring(index).split(",");
        JSONArray memory = new JSONArray();
        for (String str : split) {
            str = str.trim();
//            509248k total — 物理内存总量（509M）
//            495964k used — 使用中的内存总量（495M）
//            13284k free — 空闲内存总量（13M）
//            25364k buffers — 缓存的内存量 （25M）
            if (str.endsWith("free")) {
                memory.add(putObject("空闲内存", str.replace("free", "").trim()));
            }
            if (str.endsWith("used")) {
                memory.add(putObject("使用中的内存", str.replace("used", "").trim()));
            }
            if (str.endsWith("buff/cache")) {
                memory.add(putObject("缓存的内存", str.replace("buff/cache", "").trim()));
            }
        }
        return memory;
    }

    /**
     * 获取cpu信息
     *
     * @param info cpu信息
     * @return cpu信息
     */
    private static JSONArray getCpu(String info) {
        if (StrUtil.isEmpty(info)) {
            return null;
        }
        int i = info.indexOf(":");
        String[] split = info.substring(i + 1).split(",");
        JSONArray cpu = new JSONArray();
        for (String str : split) {
            str = str.trim();
//            1.3% us — 用户空间占用CPU的百分比。
//            1.0% sy — 内核空间占用CPU的百分比。
//            0.0% ni — 改变过优先级的进程占用CPU的百分比
//            97.3% id — 空闲CPU百分比
//            0.0% wa — IO等待占用CPU的百分比
//            0.3% hi — 硬中断（Hardware IRQ）占用CPU的百分比
//            0.0% si — 软中断（Software Interrupts）占用CPU的百分比
            String value = str.substring(0, str.length() - 2).trim();
            String tag = str.substring(str.length() - 2);
            switch (tag) {
                case "us":
                    cpu.add(putObject("用户空间", value));
                    break;
                case "sy":
                    cpu.add(putObject("内核空间", value));
                    break;
                case "ni":
                    cpu.add(putObject("改变过优先级的进程", value));
                    break;
                case "id":
                    cpu.add(putObject("空闲CPU", value));
                    break;
                case "wa":
                    cpu.add(putObject("IO等待", value));
                    break;
                case "hi":
                    cpu.add(putObject("硬中断", value));
                    break;
                case "si":
                    cpu.add(putObject("软中断", value));
                    break;
                default:
                    break;
            }
        }
        return cpu;
    }

    private static JSONObject putObject(String name, Object value) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("value", value);
        return jsonObject;
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
        int size = SESSIONS.size();
        if (size > 0) {
            return;
        }
        //
        Iterator<Session> iterator = SESSIONS.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            try {
                SocketSessionUtil.send(session, null);
            } catch (IOException e) {
                DefaultSystemLog.ERROR().error("消息失败", e);
            }
            try {
                session.close();
                iterator.remove();
            } catch (IOException ignored) {
            }
        }
        CronUtil.remove(CRON_ID);
        watch = false;
    }
}
