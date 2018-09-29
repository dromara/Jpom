package cn.jiangzeyin.socket;


import cn.jiangzeyin.common.DefaultSystemLog;

import javax.websocket.Session;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jiangzeyin on 2018/9/29.
 */
public class SocketSession {

    private TailLogThread thread;
    private Session session;

    public SocketSession(Session session) {
        this.session = session;
    }


    public TailLogThread getThread() {
        return thread;
    }

    public void setThread(TailLogThread thread) {
        this.thread = thread;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * 发送消息
     *
     * @param msg 消息
     */
    public void sendMsg(String msg) {
        if (session == null) {
            return;
        }
        try {
            DefaultSystemLog.LOG().info(msg);
            session.getBasicRemote().sendText(msg);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("websocket发送信息异常", e);
        }
    }
}
