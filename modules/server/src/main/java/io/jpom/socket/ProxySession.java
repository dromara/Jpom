package io.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.init.OperateLogController;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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
    private WebSocketSession session;
    private OperateLogController logController;

    private ProxySession(URI uri, WebSocketSession session) {
        super(uri);
        Objects.requireNonNull(session);
        this.session = session;
        this.connect();
        this.loopOpen();
        logController = SpringUtil.getBean(OperateLogController.class);
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

    public ProxySession(String uri, WebSocketSession session) throws URISyntaxException {
        this(new URI(uri), session);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            DefaultSystemLog.getLog().error("发送消息失败", e);
        }
        try {
            JSONObject jsonObject = JSONObject.parseObject(message);
            String reqId = jsonObject.getString("reqId");
            if (StrUtil.isNotEmpty(reqId)) {
                logController.updateLog(reqId, message);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        try {
            session.close();
        } catch (IOException e) {
            DefaultSystemLog.getLog().error("关闭错误", e);
        }
    }

    @Override
    public void onError(Exception ex) {
        try {
            session.sendMessage(new TextMessage("agent服务端发生异常" + ExceptionUtil.stacktraceToString(ex)));
//            SocketSessionUtil.send(session, );
        } catch (IOException ignored) {
        }
        DefaultSystemLog.getLog().error("发生错误", ex);
    }

    @Override
    public void send(String text) {
        try {
            super.send(text);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("转发消息失败", e);
        }
    }
}
