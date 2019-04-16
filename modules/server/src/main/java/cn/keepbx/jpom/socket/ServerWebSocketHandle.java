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
//            ProjectInfoService projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
            UserService userService = SpringUtil.getBean(UserService.class);
            UserModel userModel = userService.checkUser(userInfo);
            if (userModel == null) {
                SocketSessionUtil.send(session, "用户名或密码错误!");
                session.close();
                return;
            }
            NodeModel nodeModel = nodeService.getItem(nodeId);
            String url = NodeForward.getSocketUrl(nodeModel, NodeUrl.TopSocket);
            ProxySession proxySession = new ProxySession(url, session);
            PROXY_SESSION_CONCURRENT_HASH_MAP.put(session.getId(), proxySession);
            System.out.println(url);
            // 判断项目
//            if (!SYSTEM_ID.equals(projectId)) {
//                ProjectInfoModel projectInfoModel = projectInfoService.getItem(projectId);
//                if (projectInfoModel == null) {
//                    SocketSessionUtil.send(session, "获取项目信息错误");
//                    session.close();
//                    return;
//                }
//                if (!userModel.isProject(projectInfoModel.getId())) {
//                    SocketSessionUtil.send(session, "没有项目权限");
//                    session.close();
//                    return;
//                }
//            }
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
    public void onMessage(String message, Session session) {
        ProxySession proxySession = getSession(session);
        proxySession.send(message);
//        UserModel userModel = USER.get(session.getId());
//        if (userModel == null) {
//            SocketSessionUtil.send(session, "回话信息失效，刷新网页再试");
//            return;
//        }
//        System.out.println(op);
//        String projectId = json.getString("projectId");
//        ProjectInfoService projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
//        ProjectInfoModel projectInfoModel = projectInfoService.getItem(projectId);
//        ConsoleService.CommandOp commandOp = ConsoleService.CommandOp.valueOf(op);
//        if (projectInfoModel == null && commandOp != ConsoleService.CommandOp.top) {
//            SocketSessionUtil.send(session, "没有对应项目");
//            return;
//        }
//        JSONObject resultData = null;
//        String strResult;
//        boolean logUser = false;
//        try {
//            // 执行相应命令
//            switch (commandOp) {
//                case start:
//                case restart:
//                    logUser = true;
//                    strResult = consoleService.execCommand(commandOp, projectInfoModel);
//                    if (strResult.contains(ConsoleService.RUNING_TAG)) {
//                        resultData = JsonMessage.toJson(200, "操作成功:" + strResult);
//                    } else {
//                        resultData = JsonMessage.toJson(400, strResult);
//                    }
//                    break;
//                case stop:
//                    logUser = true;
//                    // 停止项目
//                    strResult = consoleService.execCommand(commandOp, projectInfoModel);
//                    if (strResult.contains(ConsoleService.STOP_TAG)) {
//                        resultData = JsonMessage.toJson(200, "操作成功");
//                    } else {
//                        resultData = JsonMessage.toJson(500, strResult);
//                    }
//                    break;
//                case status:
//                    // 获取项目状态
//                    strResult = consoleService.execCommand(commandOp, projectInfoModel);
//                    if (strResult.contains(ConsoleService.RUNING_TAG)) {
//                        resultData = JsonMessage.toJson(200, "运行中", strResult);
//                    } else {
//                        resultData = JsonMessage.toJson(404, "未运行", strResult);
//                    }
//                    break;
//                case showlog: {
//                    // 进入管理页面后需要实时加载日志
//                    String log = projectInfoModel.getLog();
//                    try {
//                        FileTailWatcher.addWatcher(log, session);
//                    } catch (IOException io) {
//                        SocketSessionUtil.send(session, io.getMessage());
//                    }
//                    break;
//                }
//                case top:
//                    TopManager.addMonitor(session);
//                    break;
//                default:
//                    resultData = JsonMessage.toJson(404, "不支持的方式：" + op);
//                    break;
//            }
//        } catch (Exception e) {
//            DefaultSystemLog.ERROR().error("执行命令失败", e);
//            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
//            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
//            return;
//        } finally {
//            if (logUser) {
//                // 记录操作人
//                projectInfoModel = projectInfoService.getItem(projectId);
//                projectInfoModel.logModifyUser(userModel);
//                projectInfoService.updateItem(projectInfoModel);
//            }
//        }
//        //
//        if (resultData != null) {
//            resultData.put("op", op);
//            DefaultSystemLog.LOG().info(resultData.toString());
//            SocketSessionUtil.send(session, resultData.toString());
//        }
    }

    private void destroy(Session session) {
        // 清理日志监听
        try {
            FileTailWatcher.offline(session);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("关闭异常", e);
        }
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