package cn.keepbx.jpom.common.forward;

import cn.keepbx.jpom.util.SocketSessionUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Created by jiangzeyin on 2019/4/16.
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
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);
        try {
            SocketSessionUtil.send(session, message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        //  log.info("[websocket] 退出连接");
        try {
            session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("退出链接");
    }

    @Override
    public void onError(Exception ex) {
        // log.info("[websocket] 连接错误={}", ex.getMessage());
        ex.printStackTrace();
    }
}
