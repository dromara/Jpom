package cn.jiangzeyin.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
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
import java.util.concurrent.ExecutorService;

/**
 * socket
 *
 * @author jiangzeyin
 * date 2017/9/8
 */
@ServerEndpoint(value = "/console/{userInfo}")
@Component
public class LogWebSocketHandle implements TailLogThread.Evn, CommandService.Evt {


    private static ExecutorService EXECUTOR_SERVICE = null;
    private Process process;
    private InputStream inputStream;
    private TailLogThread thread;
    private CommandService commandService;
    private Session session;

    /**
     * 新的WebSocket请求开启
     */
    @OnOpen
    public void onOpen(@PathParam("userInfo") String userInfo, Session session) {
        if (EXECUTOR_SERVICE == null) {
            EXECUTOR_SERVICE = ThreadPoolService.newCachedThreadPool(LogWebSocketHandle.class);
        }
        commandService = SpringUtil.getBean(CommandService.class);
        // 通过用户名和密码的Md5值判断是否是登录的
        try {
            UserService userService = SpringUtil.getBean(UserService.class);
            if (!userService.checkUser(userInfo)) {
                sendMsg(session, JsonMessage.getString(500, "用户名或密码错误!"));
                session.close();
            }
            this.session = session;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage());
            try {
                if (null != session) {
                    sendMsg(session, JsonMessage.getString(500, "系统错误!"));
                    session.close();
                }
            } catch (IOException e1) {
                DefaultSystemLog.ERROR().error(e1.getMessage(), e1);
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        DefaultSystemLog.LOG().info("客户端消息：" + message);
        JSONObject json = JSONObject.parseObject(message);
        JSONObject projectInfo = json.getJSONObject("projectInfo");
        String id = projectInfo.getString("id");
        ManageService manageService = SpringUtil.getBean(ManageService.class);
        ProjectInfoModel projectInfoModel = null;
        try {
            projectInfoModel = manageService.getProjectInfo(id);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("获取异常", e);
        }
        if (projectInfoModel == null) {
            sendMsg(session, "没有对应项目");
            return;
        }
        String op = json.getString("op");
        CommandService.CommandOp commandOp = CommandService.CommandOp.valueOf(op);
        JSONObject resultData = null;
        String strResult;
        // 执行相应命令
        switch (commandOp) {
            case start:
            case restart:
                strResult = commandService.execCommand(commandOp, projectInfoModel, this);
                if (strResult.contains(CommandService.RUNING_TAG)) {
                    resultData = JsonMessage.toJson(200, "操作成功", json);
                } else {
                    resultData = JsonMessage.toJson(400, strResult, json);
                }
                break;
            case stop:
                // 停止项目
                strResult = commandService.execCommand(commandOp, projectInfoModel, this);
                if (strResult.contains(CommandService.STOP_TAG)) {
                    resultData = JsonMessage.toJson(200, "操作成功", json);
                    if (thread != null) {
                        thread.stop();
                    }
                } else {
                    resultData = JsonMessage.toJson(500, strResult, json);
                }
                break;
            case status:
                // 获取项目状态
                strResult = commandService.execCommand(commandOp, projectInfoModel, this);
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
            sendMsg(session, resultData.toString());
        }
    }

    private void showLog(Session session, ProjectInfoModel projectInfoModel) {
        // 进入管理页面后需要实时加载日志
        String log = projectInfoModel.getLog();
        try {
            if (process != null) {
                process.destroy();
            }
            // 执行tail -f命令
            process = Runtime.getRuntime().exec(String.format("tail -f %s", log));
            if (inputStream != null) {
                inputStream.close();
            }
            inputStream = process.getInputStream();
            // 一定要启动新的线程，防止InputStream阻塞处理WebSocket的线程
            if (thread != null) {
                thread.stop();
                thread.send("停止");
            }
            thread = new TailLogThread(inputStream, log, session, this);
            EXECUTOR_SERVICE.execute(thread);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("打开日志异常", e);
            sendMsg(session, "打开日志异常");
        }
    }


    /**
     * WebSocket请求关闭
     */
    @OnClose
    public void onClose() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (process != null) {
                process.destroy();
            }
            if (thread != null) {
                thread.stop();
            }
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("关闭异常", e);
        }
        DefaultSystemLog.LOG().info(" socket 关闭");
    }

    /**
     * 发送消息
     *
     * @param session 回话
     * @param msg     消息
     */
    private void sendMsg(Session session, String msg) {
        if (session == null) {
            return;
        }
        try {
            DefaultSystemLog.LOG().info(msg);
            session.getBasicRemote().sendText(msg);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error("websocket发送信息异常", e);
        }
    }

    @OnError
    public void onError(Session session, Throwable thr) {
        DefaultSystemLog.ERROR().error("socket 异常", thr);
    }

    @Override
    public void onError() {
        onClose();
    }

    @Override
    public void commandError(Exception e) {
        if (session == null) {
            return;
        }
        sendMsg(session, "执行命令异常：" + ExceptionUtil.stacktraceToString(e));
    }

    public static void main(String[] args) {
        try {
            String command = "/boot-line/command/run_boot.sh status Testteststesttst cn.jiangzeyin.BootOnLineApplication /boot-line/boot/online/lib /boot-line/boot/online/run.log no";
//            String command = "ps -ef";
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            InputStream is;
            int wait = process.waitFor();
            if (wait == 0) {
                is = process.getInputStream();
            } else {
                is = process.getErrorStream();
            }
            System.out.println(IoUtil.read(is, CharsetUtil.CHARSET_UTF_8));
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}