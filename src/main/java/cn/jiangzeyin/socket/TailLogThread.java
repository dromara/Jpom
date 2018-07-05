package cn.jiangzeyin.socket;

import cn.jiangzeyin.common.DefaultSystemLog;

import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 线程处理
 * Created by jiangzeyin on 2017/9/8.
 */
public class TailLogThread implements Runnable {

    private BufferedReader reader;
    private final Session session;
    private boolean run = false;
    private Evn evn;

    public TailLogThread(InputStream in, Session session, Evn evn) {
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.session = session;
        this.evn = evn;
    }

    public void stop() {
        run = false;
    }

    public boolean isRun() {
        return run;
    }

    @Override
    public void run() {
        String line;
        int errorCount = 0;
        run = true;
        while (run) {
            synchronized (session) {
                // 将实时日志通过WebSocket发送给客户端，给每一行添加一个HTML换行
                try {
                    line = reader.readLine();
                    if (line != null) {
                        session.getBasicRemote().sendText(line);
                    }
                } catch (Exception e) {
                    DefaultSystemLog.ERROR().error("发送消息失败", e);
                    errorCount++;
                    if (errorCount == 10) {
                        DefaultSystemLog.LOG().info("失败次数超过10次，结束本次事件");
                        stop();
                        if (evn != null)
                            evn.onError();
                        break;
                    }
                }
            }
        }
        DefaultSystemLog.LOG().info("结束本次读取地址事件");
    }

    public interface Evn {
        void onError();
    }
}