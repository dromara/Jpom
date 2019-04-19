package cn.keepbx.jpom.common.forward;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.util.SocketSessionUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * 代理socket 会话
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class ProxySession extends WebSocketClient {
    private Session session;

    private ProxySession(URI uri, Session session) {
        super(uri);
        Objects.requireNonNull(session);
        this.session = session;
        this.connect();
        this.loopOpen();
    }

    /**
     * 等待连接成功
     */
    private void loopOpen() {
        int count = 0;
        while (!this.isOpen() && count < 20) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            count++;
        }
    }

    public ProxySession(String uri, Session session) throws URISyntaxException {
        this(new URI(uri), session);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String message) {
        try {
            SocketSessionUtil.send(session, message);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("发送消息失败", e);
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        try {
            session.close();
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("关闭错误", e);
        }
    }

    @Override
    public void onError(Exception ex) {
        try {
            SocketSessionUtil.send(session, "agent服务端发生异常" + ExceptionUtil.stacktraceToString(ex));
        } catch (IOException ignored) {
        }
        DefaultSystemLog.ERROR().error("发生错误", ex);
    }
}
