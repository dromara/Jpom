package io.jpom.util;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.websocket.Session;
import java.io.IOException;

/**
 * socket 会话对象
 *
 * @author jiangzeyin
 * @date 2018/9/29
 */
public class SocketSessionUtil {
    /**
     * 锁
     */
    private static final KeyLock<String> LOCK = new KeyLock<>();
    /**
     * 错误尝试次数
     */
    private static final int ERROR_TRY_COUNT = 10;

    /**
     * 发送消息
     *
     * @param session 会话对象
     * @param msg     消息
     * @throws IOException 异常
     */
    public static void send(final Session session, String msg) throws IOException {
        if (StrUtil.isEmpty(msg)) {
            return;
        }
        if (!session.isOpen()) {
            throw new RuntimeException("session close ");
        }
        try {
            LOCK.lock(session.getId());
            IOException exception = null;
            int tryCount = 0;
            do {
                tryCount++;
                if (exception != null) {
                    // 上一次有异常、休眠 500
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    session.getBasicRemote().sendText(msg);
                    exception = null;
                    break;
                } catch (IOException e) {
                    DefaultSystemLog.getLog().error("发送消息失败:" + tryCount, e);
                    exception = e;
                }
            } while (tryCount <= ERROR_TRY_COUNT);
            if (exception != null) {
                throw exception;
            }
        } finally {
            LOCK.unlock(session.getId());
        }
    }

    public static void send(WebSocketSession session, String msg) throws IOException {
        if (StrUtil.isEmpty(msg)) {
            return;
        }
        if (!session.isOpen()) {
            throw new RuntimeException("session close ");
        }
        try {
            LOCK.lock(session.getId());
            IOException exception = null;
            int tryCount = 0;
            do {
                tryCount++;
                if (exception != null) {
                    // 上一次有异常、休眠 500
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
                try {
                    session.sendMessage(new TextMessage(msg));
                    exception = null;
                    break;
                } catch (IOException e) {
                    DefaultSystemLog.getLog().error("发送消息失败:" + tryCount, e);
                    exception = e;
                }
            } while (tryCount <= ERROR_TRY_COUNT);
            if (exception != null) {
                throw exception;
            }
        } finally {
            LOCK.unlock(session.getId());
        }
    }
}
