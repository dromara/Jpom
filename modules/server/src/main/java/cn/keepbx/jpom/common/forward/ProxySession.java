package cn.keepbx.jpom.common.forward;

import cn.keepbx.jpom.util.SocketSessionUtil;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by jiangzeyin on 2019/4/16.
 */
public class ProxySession extends WebSocketClient {
    private WebSocketClient webSocketClient;
    private Session session;

    public ProxySession(URI uri, Session session) throws URISyntaxException {
        super(uri);
        this.session = session;
        this.connect();
    }

    public ProxySession(String uri, Session session) throws URISyntaxException {
        this(new URI(uri), session);
    }


//    public static ProxySession carete(String url, Session session) throws URISyntaxException {
//        WebSocketClient webSocketClient = new WebSocketClient(, new Draft_6455()) {
//
//        };
//
//    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {
        try {
            SocketSessionUtil.send(session, message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        //  log.info("[websocket] 退出连接");
    }

    @Override
    public void onError(Exception ex) {
        // log.info("[websocket] 连接错误={}", ex.getMessage());
    }
}
