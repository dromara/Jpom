package cn.jiangzeyin.socket;

import cn.jiangzeyin.system.log.SystemLog;

import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by jiangzeyin on 2017/9/8.
 */
public class TailLogThread implements Runnable {

    private BufferedReader reader;
    private Session session;
    private boolean run = true;
    private Evn evn;

    public TailLogThread(InputStream in, Session session, Evn evn) {
        this.reader = new BufferedReader(new InputStreamReader(in));
        this.session = session;
        this.evn = evn;

    }

    public void stop() {
        run = false;
    }

    @Override
    public void run() {
        String line;
        int errorCount = 0;
        try {
            while (run && (line = reader.readLine()) != null) {
                // 将实时日志通过WebSocket发送给客户端，给每一行添加一个HTML换行
                try {
                    session.getBasicRemote().sendText(line + "<br>");
                } catch (Exception e) {
                    SystemLog.ERROR().error("发送消息失败", e);
                    errorCount++;
                    if (errorCount == 10) {
                        SystemLog.LOG().info("失败次数超过10次，结束本次事件");
                        if (evn != null)
                            evn.onError();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            SystemLog.ERROR().error("读取异常", e);
        }
        SystemLog.LOG().info("结束本次读取地址事件");
    }

    public interface Evn {
        void onError();
    }
}