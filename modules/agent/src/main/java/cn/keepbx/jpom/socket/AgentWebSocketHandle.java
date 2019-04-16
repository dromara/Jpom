package cn.keepbx.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.system.TopManager;
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
    public static final String SYSTEM_ID = "system";


    @OnOpen
    public void onOpen(@PathParam("projectId") String projectId, Session session) {
        System.out.println(projectId);
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JSONObject json = JSONObject.parseObject(message);
        String op = json.getString("op");
        CommandOp commandOp = CommandOp.valueOf(op);
        if (commandOp == CommandOp.heart) {
            return;
        }
        if (commandOp == CommandOp.top) {
            TopManager.addMonitor(session);
            return;
        }
    }

    @OnClose
    public void onClose(Session session) {
        // top
        TopManager.removeMonitor(session);
        System.out.println("close");
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
