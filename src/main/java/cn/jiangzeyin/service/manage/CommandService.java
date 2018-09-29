package cn.jiangzeyin.service.manage;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.socket.LogWebSocketHandle;
import cn.jiangzeyin.socket.SocketSession;
import org.springframework.stereotype.Service;

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jiangzeyin on 2018/9/28.
 */
@Service
public class CommandService {
    public static final String RUNING_TAG = "running";
    public static final String STOP_TAG = "stopped";

    public enum CommandOp {
        /**
         * 启动
         */
        start,
        stop,
        restart,
        status,
        showlog
    }


    public File getCommandFile() {
        File file = new File(getCommandPath());
        if (!file.exists()) {
            throw new RuntimeException("启动文件不存在");
        }
        return file;
    }

    private String getCommandPath() {
        String command = SpringUtil.getEnvironment().getProperty("boot-online.command");
        if (StrUtil.isEmpty(command)) {
            throw new RuntimeException("请配置命令文件");
        }
        return command;
    }

    /**
     * 执行shell命令
     *
     * @param commandOp        执行的操作
     * @param projectInfoModel 项目信息
     */
    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel, Evt evt) {
        String result = "error";
        if (commandOp == CommandOp.showlog) {
            return result;
        }
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
            InputStream is;
            // 执行命令
            String command = String.format("%s %s %s %s %s %s %s [%s][%s]", commandPath, commandOp.toString(), tag, mainClass, lib, log, token, jvm, args);
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
            if (evt != null) {
                evt.commandError(e);
            }
        }
        return result;
    }

    public interface Evt {
        /**
         * 执行异常
         *
         * @param e e
         */
        void commandError(Exception e);
    }

    public static class EvtIml implements Evt {

        private Session session;

        public EvtIml(Session session) {
            this.session = session;
        }

        @Override
        public void commandError(Exception e) {
            SocketSession socketSession = LogWebSocketHandle.SESSION_CONCURRENT_HASH_MAP.get(session.getId());
            if (socketSession != null) {
                socketSession.sendMsg("执行命令异常：" + ExceptionUtil.stacktraceToString(e));
            }
        }
    }
}
