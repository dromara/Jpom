//package cn.keepbx.jpom.socket;
//
//import cn.hutool.core.exceptions.ExceptionUtil;
//import cn.hutool.core.util.IdUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.core.util.URLUtil;
//import cn.jiangzeyin.common.DefaultSystemLog;
//import cn.jiangzeyin.common.JsonMessage;
//import cn.jiangzeyin.common.spring.SpringUtil;
//import cn.keepbx.jpom.common.forward.NodeForward;
//import cn.keepbx.jpom.common.forward.NodeUrl;
//import cn.keepbx.jpom.socket.ProxySession;
//import cn.keepbx.jpom.model.data.NodeModel;
//import cn.keepbx.jpom.model.data.UserModel;
//import cn.keepbx.jpom.model.data.UserOperateLogV1;
//import cn.keepbx.jpom.service.node.NodeService;
//import cn.keepbx.jpom.service.user.UserService;
//import cn.keepbx.jpom.system.init.OperateLogController;
//import cn.keepbx.jpom.util.SocketSessionUtil;
//import com.alibaba.fastjson.JSONObject;
//
//import javax.websocket.*;
//import javax.websocket.server.PathParam;
//import java.io.IOException;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * socket 消息控制器
// *
// * @author jiangzeyin
// * @date 2017/9/8
// */
////@ServerEndpoint(value = "/console/{nodeId}/{userInfo}/{projectId}")
////@Component
//public class ServerWebSocketHandle {
//
//    private NodeService nodeService;
//    private OperateLogController operateLogController;
//    private static volatile AtomicInteger onlineCount = new AtomicInteger();
//    private static final ConcurrentHashMap<String, CacheInfo> CACHE_INFO_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
//    private static final ConcurrentHashMap<String, ProxySession> PROXY_SESSION_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();
//
//    /**
//     * 新的WebSocket请求开启
//     *
//     * @param userInfo  用户授权信息
//     * @param projectId 项目id
//     * @param session   会话
//     */
//    @OnOpen
//    public void onOpen(@PathParam("nodeId") String nodeId, @PathParam("userInfo") String userInfo, @PathParam("projectId") String projectId, Session session) {
//        if (nodeService == null) {
//            nodeService = SpringUtil.getBean(NodeService.class);
//        }
//        if (operateLogController == null) {
//            operateLogController = SpringUtil.getBean(OperateLogController.class);
//        }
//        // 通过用户名和密码的Md5值判断是否是登录的
//        try {
//            UserService userService = SpringUtil.getBean(UserService.class);
//            UserModel userModel = userService.checkUser(userInfo);
//            if (userModel == null) {
//                SocketSessionUtil.send(session, "用户名或密码错误!");
//                session.close();
//                return;
//            }
//            String userName = UserModel.getOptUserName(userModel);
//            userName = URLUtil.encode(userName);
//            NodeModel nodeModel = nodeService.getItem(nodeId);
//            // 判断权限
//            if (!userModel.isProject(nodeModel.getId(), projectId)) {
//                SocketSessionUtil.send(session, "没有项目权限");
//                return;
//            }
//            String url = NodeForward.getSocketUrl(nodeModel, NodeUrl.TopSocket);
//            url = StrUtil.format(url, projectId, userName);
//            ProxySession proxySession = null;
//            //new ProxySession(url, session);
//            PROXY_SESSION_CONCURRENT_HASH_MAP.put(session.getId(), proxySession);
//            SocketSessionUtil.send(session, StrUtil.format("欢迎加入:{} 会话id:{} 当前会话总数:{}", userModel.getName(), session.getId(), onlineCount.incrementAndGet()));
//            CacheInfo cacheInfo = new CacheInfo();
//            cacheInfo.userModel = userModel;
//            cacheInfo.nodeModel = nodeModel;
//            CACHE_INFO_CONCURRENT_HASH_MAP.put(session.getId(), cacheInfo);
//        } catch (Exception e) {
//            DefaultSystemLog.ERROR().error("socket 错误", e);
//            try {
//                SocketSessionUtil.send(session, JsonMessage.getString(500, "系统错误!"));
//                session.close();
//            } catch (IOException e1) {
//                DefaultSystemLog.ERROR().error(e1.getMessage(), e1);
//            }
//        }
//    }
//
//    private ProxySession getSession(Session session) {
//        return PROXY_SESSION_CONCURRENT_HASH_MAP.get(session.getId());
//    }
//
//    @OnMessage
//    public void onMessage(String message, Session session) throws IOException {
//        CacheInfo cacheInfo = CACHE_INFO_CONCURRENT_HASH_MAP.get(session.getId());
//        if (cacheInfo == null) {
//            SocketSessionUtil.send(session, "会话信息失效，刷新网页再试");
//            session.close();
//            return;
//        }
//        ProxySession proxySession = getSession(session);
//        JSONObject json = JSONObject.parseObject(message);
//        String op = json.getString("op");
//        CommandOp commandOp = CommandOp.valueOf(op);
//        UserOperateLogV1.OptType type = null;
//        switch (commandOp) {
//            case stop:
//                type = UserOperateLogV1.OptType.Stop;
//                break;
//            case start:
//                type = UserOperateLogV1.OptType.Start;
//                break;
//            case restart:
//                type = UserOperateLogV1.OptType.Restart;
//                break;
//            default:
//                break;
//        }
//        if (type != null) {
//            json.put("reqId", IdUtil.fastUUID());
//            operateLogController.log(cacheInfo.userModel, "还没有响应", "", type, cacheInfo.nodeModel);
//            proxySession.send(json.toString());
//        } else {
//            proxySession.send(message);
//        }
//    }
//
//    private void destroy(Session session) {
//        ProxySession proxySession = getSession(session);
//        if (proxySession != null) {
//            proxySession.close();
//        }
//        onlineCount.getAndDecrement();
//    }
//
//    /**
//     * WebSocket请求关闭
//     */
//    @OnClose
//    public void onClose(Session session) {
//        try {
//            destroy(session);
//        } catch (Exception e) {
//            DefaultSystemLog.ERROR().error("关闭异常", e);
//        }
//        CACHE_INFO_CONCURRENT_HASH_MAP.remove(session.getId());
//        DefaultSystemLog.LOG().info(session.getId() + " socket 关闭");
//    }
//
//
//    @OnError
//    public void onError(Session session, Throwable thr) {
//        // java.io.IOException: Broken pipe
//        try {
//            SocketSessionUtil.send(session, "服务端发生异常" + ExceptionUtil.stacktraceToString(thr));
//        } catch (IOException ignored) {
//        }
//        DefaultSystemLog.ERROR().error(session.getId() + "socket 异常", thr);
//    }
//
//    private static class CacheInfo {
//        private UserModel userModel;
//        private NodeModel nodeModel;
//    }
//}