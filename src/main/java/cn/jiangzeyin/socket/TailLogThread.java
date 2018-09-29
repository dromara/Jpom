package cn.jiangzeyin.socket;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;

import javax.websocket.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程处理
 *
 * @author jiangzeyin
 * date 2017/9/8
 */
public class TailLogThread implements Runnable {

    private static ConcurrentHashMap<String, InputStream> MAP = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Boolean> CHANGE = new ConcurrentHashMap<>();

    private final Session session;
    private boolean run = true;
    private Evn evn;
    private String log;
    private int errorCount;

    TailLogThread(InputStream in, String log, Session session, Evn evn) {
        this.session = session;
        this.evn = evn;
        this.log = log;
        MAP.put(log, in);
        CHANGE.put(log, true);
    }

    void stop() {
        run = false;
    }

    public void send(String msg) {
        if (msg == null) {
            return;
        }
        if (!session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(msg);
            }
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("发送消息失败", e);
            errorCount++;
            if (errorCount >= 10) {
                DefaultSystemLog.LOG().info("失败次数超过大于10次，结束本次事件");
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void run() {
        InputStream inputStream = MAP.get(this.log);
        while (run) {
            Boolean change = CHANGE.get(this.log);
            if (change != null && change) {
                inputStream = MAP.get(this.log);
                CHANGE.remove(this.log);
                send("自动切换到新的流");
                System.out.println("切换：" + session.getId());
            }
            if (inputStream == null) {
                send("没有对应日志信息");
                break;
            }
            try {
                errorCount = 0;
                // 将实时日志通过WebSocket发送给客户端，给每一行添加一个HTML换行
                IoUtil.readLines(inputStream, CharsetUtil.CHARSET_UTF_8, (LineHandler) this::send);
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg.contains("Stream closed") && errorCount++ <= 10) {
                    send("流已关闭,将等等自动重连");
                    continue;
                }
                e.printStackTrace();
                stop();
                if (evn != null) {
                    evn.onError(session);
                }
                break;
            }
        }
        DefaultSystemLog.LOG().info("结束本次读取地址事件");
    }

    public interface Evn {
        /**
         * 尝试次数过多
         *
         * @param session 会话
         */
        void onError(Session session);
    }
}