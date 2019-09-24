package io.jpom.socket;

import cn.jiangzeyin.common.DefaultSystemLog;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
public abstract class BaseHandler extends TextWebSocketHandler {

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        DefaultSystemLog.getLog().error(session.getId() + "socket 异常", exception);
        destroy(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        destroy(session);
    }

    /**
     * 关闭连接
     *
     * @param session session
     */
    public abstract void destroy(WebSocketSession session);
}
