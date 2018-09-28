package cn.jiangzeyin.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.pool.ThreadPoolService;
import cn.jiangzeyin.service.BaseService;
import cn.jiangzeyin.service.manage.CommandService;
import cn.jiangzeyin.service.manage.ManageService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * socket
 * Created by jiangzeyin on 2017/9/8.
 */
@ServerEndpoint(value = "/console/{userInfo}")
@Component
public class LogWebSocketHandle implements TailLogThread.Evn {

    private static ExecutorService EXECUTOR_SERVICE = null;

    private Process process;
    private InputStream inputStream;
    private TailLogThread thread;

    /**
     * 新的WebSocket请求开启
     */
    @OnOpen
    public void onOpen(@PathParam("userInfo") String userInfo, Session session) {
        if (EXECUTOR_SERVICE == null) {
            EXECUTOR_SERVICE = ThreadPoolService.newCachedThreadPool(LogWebSocketHandle.class);
        }
        // 通过用户名和密码的Md5值判断是否是登录的
        try {
            boolean flag = false;
            if (!StrUtil.isEmpty(userInfo)) {
                BaseService service = new BaseService();
                JSONObject obj = service.getJsonObject("user.json");

                Set<String> set_key = obj.keySet();

                for (String str_key : set_key) {
                    JSONObject json_user = obj.getJSONObject(str_key);
                    String str_userMd5 = SecureUtil.md5(String.format("%s:%s", json_user.getString("id"), json_user.getString("password")));
                    if (str_userMd5.equals(userInfo)) {
                        flag = true;
                        break;
                    }
                }
            } else {
                sendMsg(session, JsonMessage.getString(500, "用户名或密码错误!"));
                session.close();
            }
            if (!flag) {
                sendMsg(session, JsonMessage.getString(500, "用户名或密码错误!"));
                session.close();
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage());
            try {
                if (null != session) {
                    sendMsg(session, JsonMessage.getString(500, "系统错误!"));
                    session.close();
                }
            } catch (IOException e1) {
                DefaultSystemLog.ERROR().error(e1.getMessage());
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {

        DefaultSystemLog.LOG().info("客户端消息：" + message);
        JSONObject json = JSONObject.parseObject(message);
        JSONObject projectInfo = json.getJSONObject("projectInfo");

        String str_result;
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
        JSONObject resultData = null;
        // 执行相应命令
        switch (op) {
            case "start":
                // 启动项目
                str_result = execCommand(session, "start", projectInfoModel);
                if (str_result.contains("running")) {
                    resultData = JsonMessage.toJson(200, "启动成功", json);
                } else {
                    resultData = JsonMessage.toJson(400, str_result, json);
                }
                break;

            case "restart":
                // 重启项目
                str_result = execCommand(session, "restart", projectInfoModel);
                if (str_result.contains("running")) {
                    resultData = JsonMessage.toJson(200, "重启成功", json);
                } else {
                    resultData = JsonMessage.toJson(400, str_result, json);
                }
                break;

            case "stop":
                // 停止项目
                str_result = execCommand(session, "stop", projectInfoModel);
                if (str_result.contains("stopped")) {
                    resultData = JsonMessage.toJson(200, "已停止", json);
                    thread.stop();
                } else {
                    resultData = JsonMessage.toJson(500, str_result, json);
                }
                break;

            case "status":
                // 获取项目状态
                str_result = execCommand(session, "status", projectInfoModel);
                json.put("result", str_result);
                if (str_result.contains("running")) {
                    resultData = JsonMessage.toJson(200, "运行中", json);
                } else {
                    resultData = JsonMessage.toJson(404, "未运行", json);
                }
                break;
            case "showlog":
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
                    }
                    thread = new TailLogThread(inputStream, session, this);
                    EXECUTOR_SERVICE.execute(thread);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error("打开日志异常", e);
                }
                break;
            default:
                break;
        }
        if (resultData != null) {
            resultData.put("op", op);
            sendMsg(session, resultData.toString());
        }
    }

    /**
     * 执行shell命令
     *
     * @param session          用于输出的websocket会话
     * @param op               执行的操作
     * @param projectInfoModel 项目信息
     */
    private String execCommand(Session session, String op, ProjectInfoModel projectInfoModel) {
        InputStream is;
        String result = "error";
        CommandService commandService = SpringUtil.getBean(CommandService.class);
        String commandPath = commandService.getCommandPath();

        // 项目启动信息
        String tag = projectInfoModel.getTag();
        String mainClass = projectInfoModel.getMainClass();
        String lib = projectInfoModel.getLib();
        String log = projectInfoModel.getLog();
        String token = projectInfoModel.getToken();
        String jvm = projectInfoModel.getJvm();
        String args = projectInfoModel.getArgs();
        try {
            // 执行命令
            String command = String.format("%s %s %s %s %s %s %s [%s][%s]", commandPath, op, tag, mainClass, lib, log, token, jvm, args);
            DefaultSystemLog.LOG().info(command);
            Process process = Runtime.getRuntime().exec(command);
            int wait = process.waitFor();
            if (wait == 0) {
                is = process.getInputStream();
            } else {
                is = process.getErrorStream();
            }
            result = IoUtil.read(is, CharsetUtil.CHARSET_UTF_8);
            is.close();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            sendMsg(session, "执行命令异常：" + ExceptionUtil.stacktraceToString(e));
        }
        return result;
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
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("关闭异常", e);
        }
        if (process != null) {
            process.destroy();
        }
        if (thread != null) {
            thread.stop();
        }
        DefaultSystemLog.LOG().info(" socket 关闭");
    }

    /**
     * 发送消息
     *
     * @param session
     * @param msg
     */
    private synchronized void sendMsg(Session session, String msg) {
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