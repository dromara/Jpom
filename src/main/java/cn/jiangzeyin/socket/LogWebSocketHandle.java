package cn.jiangzeyin.socket;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.pool.ThreadPoolService;
import cn.jiangzeyin.service.UserService;
import cn.jiangzeyin.service.manage.CommandService;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * socket
 *
 * @author jiangzeyin
 * date 2017/9/8
 */
@ServerEndpoint(value = "/console/{userInfo}")
@Component
public class LogWebSocketHandle implements TailLogThread.Evn {

    private static ExecutorService EXECUTOR_SERVICE = null;
    private CommandService commandService;
    public static final ConcurrentHashMap<String, SocketSession> SESSION_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    /**
     * 新的WebSocket请求开启
     */
    @OnOpen
    public void onOpen(@PathParam("userInfo") String userInfo, Session session) {
        if (EXECUTOR_SERVICE == null) {
            EXECUTOR_SERVICE = ThreadPoolService.newCachedThreadPool(LogWebSocketHandle.class);
        }
        commandService = SpringUtil.getBean(CommandService.class);
        SocketSession socketSession = getItem(session);
        // 通过用户名和密码的Md5值判断是否是登录的
        try {
            UserService userService = SpringUtil.getBean(UserService.class);
            if (!userService.checkUser(userInfo)) {
                socketSession.sendMsg(JsonMessage.getString(500, "用户名或密码错误!"));
                session.close();
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            try {
                socketSession.sendMsg(JsonMessage.getString(500, "系统错误!"));
                session.close();
            } catch (IOException e1) {
                DefaultSystemLog.ERROR().error(e1.getMessage(), e1);
            }
        }
    }

    private SocketSession getItem(Session session) {
        SocketSession socketSession = SESSION_CONCURRENT_HASH_MAP.get(session.getId());
        if (socketSession == null) {
            socketSession = new SocketSession(session);
            SESSION_CONCURRENT_HASH_MAP.put(session.getId(), socketSession);
        }
        return socketSession;
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        DefaultSystemLog.LOG().info("客户端消息：" + message);
        JSONObject json = JSONObject.parseObject(message);
        JSONObject projectInfo = json.getJSONObject("projectInfo");
        String id = projectInfo.getString("id");
        ManageService manageService = SpringUtil.getBean(ManageService.class);
        SocketSession socketSession = getItem(session);
        ProjectInfoModel projectInfoModel = null;
        try {
            projectInfoModel = manageService.getProjectInfo(id);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("获取异常", e);
        }
        if (projectInfoModel == null) {
            socketSession.sendMsg("没有对应项目");
            return;
        }
        String op = json.getString("op");
        CommandService.CommandOp commandOp = CommandService.CommandOp.valueOf(op);
        JSONObject resultData = null;
        String strResult;
        CommandService.EvtIml evtIml = new CommandService.EvtIml(session);
        // 执行相应命令
        switch (commandOp) {
            case start:
            case restart:
                strResult = commandService.execCommand(commandOp, projectInfoModel, evtIml);
                if (strResult.contains(CommandService.RUNING_TAG)) {
                    resultData = JsonMessage.toJson(200, "操作成功", json);
                } else {
                    resultData = JsonMessage.toJson(400, strResult, json);
                }
                break;
            case stop:
                // 停止项目
                strResult = commandService.execCommand(commandOp, projectInfoModel, evtIml);
                if (strResult.contains(CommandService.STOP_TAG)) {
                    resultData = JsonMessage.toJson(200, "操作成功", json);
                } else {
                    resultData = JsonMessage.toJson(500, strResult, json);
                }
                break;
            case status:
                // 获取项目状态
                strResult = commandService.execCommand(commandOp, projectInfoModel, evtIml);
                json.put("result", strResult);
                if (strResult.contains(CommandService.RUNING_TAG)) {
                    resultData = JsonMessage.toJson(200, "运行中", json);
                } else {
                    resultData = JsonMessage.toJson(404, "未运行", json);
                }
                break;
            case showlog:
                showLog(session, projectInfoModel);
                break;
            default:
                resultData = JsonMessage.toJson(404, "不支持的方式：" + op);
                break;
        }
        if (resultData != null) {
            resultData.put("op", op);
            socketSession.sendMsg(resultData.toString());
        }
    }

    private void showLog(Session session, ProjectInfoModel projectInfoModel) {
        SocketSession socketSession = getItem(session);
        // 进入管理页面后需要实时加载日志
        String log = projectInfoModel.getLog();
        try {
            destroy(session, "重新打开新的对话");
            // 一定要启动新的线程，防止InputStream阻塞处理WebSocket的线程
            TailLogThread thread = TailLogThread.createThread(session, log, this);
            socketSession.setThread(thread);
            EXECUTOR_SERVICE.execute(thread);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("打开日志异常", e);
            socketSession.sendMsg("打开日志异常");
        }
    }


    private void destroy(Session session, String callback) {
        try {
            SocketSession socketSession = getItem(session);
            if (socketSession.getThread() != null) {
                if (callback != null) {
                    socketSession.getThread().send(callback);
                }
                socketSession.getThread().stop();
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("关闭异常", e);
        }
    }

    /**
     * WebSocket请求关闭
     */
    @OnClose
    public void onClose(Session session) {
        try {
            destroy(session, null);
            SESSION_CONCURRENT_HASH_MAP.remove(session.getId());
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("关闭异常", e);
        }
        DefaultSystemLog.LOG().info(session.getId() + " socket 关闭");
    }


    @OnError
    public void onError(Session session, Throwable thr) {
        DefaultSystemLog.ERROR().error(session.getId() + "socket 异常", thr);
    }

    @Override
    public void onError(Session session) {
        onClose(session);
    }
}