package cn.jiangzeyin.socket;

import cn.jiangzeyin.system.log.SystemLog;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jiangzeyin on 2017/9/8.
 */
@ServerEndpoint("/log")
@Component
public class LogWebSocketHandle implements TailLogThread.Evn {

    private Process process;
    private InputStream inputStream;
    private TailLogThread thread;

    /**
     * 新的WebSocket请求开启
     */
    @OnOpen
    public void onOpen(Session session) {
        try {
            SystemLog.LOG().info("创建 socket id  " + session.getId());
            // 执行tail -f命令
            process = Runtime.getRuntime().exec("tail -f /yokead_boot/nginx/run.log");
            inputStream = process.getInputStream();
            // 一定要启动新的线程，防止InputStream阻塞处理WebSocket的线程
            thread = new TailLogThread(inputStream, session, this);
            Thread thread_ = new Thread(thread);
            thread_.start();
        } catch (IOException e) {
            SystemLog.ERROR().error("打开异常", e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        SystemLog.LOG().info("客户端消息：" + message);
    }

    /**
     * WebSocket请求关闭
     */
    @OnClose
    public void onClose() {
        try {
            if (inputStream != null)
                inputStream.close();
        } catch (Exception e) {
            SystemLog.ERROR().error("关闭异常", e);
        }
        if (process != null)
            process.destroy();
        if (thread != null)
            thread.stop();
        SystemLog.LOG().info(" socket 关闭");
    }

    @OnError
    public void onError(Throwable thr) {
        onClose();
        SystemLog.ERROR().error("socket 异常", thr);
    }

    @Override
    public void onError() {
        onClose();
    }
}