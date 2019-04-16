package cn.keepbx.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.util.SocketSessionUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@ServerEndpoint(value = "/console/{projectId}")
@Component
public class AgentWebSocketHandle {

    @OnOpen
    public void onOpen(@PathParam("projectId") String projectId, Session session) {

    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JSONObject json = JSONObject.parseObject(message);
        String op = json.getString("op");
        if ("heart".equals(op)) {
            return;
        }
    }

    @OnClose
    public void onClose(Session session) {

    }

    @OnError
    public void onError(Session session, Throwable thr) {
        // java.io.IOException: Broken pipe
        try {
            SocketSessionUtil.send(session, "服务端发生异常" + ExceptionUtil.stacktraceToString(thr));
        } catch (IOException ignored) {
        }
        DefaultSystemLog.ERROR().error(session.getId() + "socket 异常", thr);
    }
}
