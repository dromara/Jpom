package cn.keepbx.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.common.forward.ProxySession;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.util.SocketSessionUtil;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * socket 消息控制器
 *
 * @author jiangzeyin
 * @date 2017/9/8
 */
@ServerEndpoint(value = "/{nodeId}/console/{userInfo}/{projectId}")
@Component
public class ServerWebSocketHandle {

    private NodeService nodeService;
    private static volatile AtomicInteger onlineCount = new AtomicInteger();
    private static final ConcurrentHashMap<String, UserModel> USER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ProxySession> PROXY_SESSION_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    /**
     * 新的WebSocket请求开启
     *
     * @param userInfo  用户授权信息
     * @param projectId 项目id
     * @param session   回话
     */
    @OnOpen
    public void onOpen(@PathParam("nodeId") String nodeId, @PathParam("userInfo") String userInfo, @PathParam("projectId") String projectId, Session session) {
        if (nodeService == null) {
            nodeService = SpringUtil.getBean(NodeService.class);
        }
        // 通过用户名和密码的Md5值判断是否是登录的
        try {
            UserService userService = SpringUtil.getBean(UserService.class);
            UserModel userModel = userService.checkUser(userInfo);
            if (userModel == null) {
                SocketSessionUtil.send(session, "用户名或密码错误!");
                session.close();
                return;
            }
            NodeModel nodeModel = nodeService.getItem(nodeId);
            String url = NodeForward.getSocketUrl(nodeModel, NodeUrl.TopSocket);
            url = StrUtil.format(url, projectId);
            ProxySession proxySession = new ProxySession(url, session);
            PROXY_SESSION_CONCURRENT_HASH_MAP.put(session.getId(), proxySession);
            System.out.println(url);

            SocketSessionUtil.send(session, StrUtil.format("欢迎加入:{} 回话id:{} 当前会话总数:{}", userModel.getName(), session.getId(), onlineCount.incrementAndGet()));
            USER.put(session.getId(), userModel);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("socket 错误", e);
            try {
                SocketSessionUtil.send(session, JsonMessage.getString(500, "系统错误!"));
                session.close();
            } catch (IOException e1) {
                DefaultSystemLog.ERROR().error(e1.getMessage(), e1);
            }
        }
    }

    private ProxySession getSession(Session session) {
        return PROXY_SESSION_CONCURRENT_HASH_MAP.get(session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        UserModel userModel = USER.get(session.getId());
        if (userModel == null) {
            SocketSessionUtil.send(session, "回话信息失效，刷新网页再试");
            return;
        }
        ProxySession proxySession = getSession(session);
        proxySession.send(message);
    }

    private void destroy(Session session) {
        ProxySession proxySession = getSession(session);
        proxySession.close();
        onlineCount.getAndDecrement();
    }

    /**
     * WebSocket请求关闭
     */
    @OnClose
    public void onClose(Session session) {
        try {
            destroy(session);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("关闭异常", e);
        }
        USER.remove(session.getId());
        DefaultSystemLog.LOG().info(session.getId() + " socket 关闭");
    }


    @OnError
    public void onError(Session session, Throwable thr) {
        // java.io.IOException: Broken pipe
        try {
            SocketSessionUtil.send(session, "服务端发生异常" + ExceptionUtil.stacktraceToString(thr));
        } catch (IOException ignored) {
        }
        USER.remove(session.getId());
        DefaultSystemLog.ERROR().error(session.getId() + "socket 异常", thr);
    }
}