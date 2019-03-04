package cn.keepbx.jpom.service.manage;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.socket.LogWebSocketHandle;
import cn.keepbx.jpom.socket.SocketSession;
import cn.keepbx.jpom.socket.TailLogThread;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ConfigException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.websocket.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Created by jiangzeyin on 2018/9/28.
 *
 * @author jiangzeyin
 */
@Service
public class CommandService {
    public static final String RUNING_TAG = "running";
    public static final String STOP_TAG = "stopped";

    @Resource
    private ProjectInfoService projectInfoService;


    public enum CommandOp {
        /**
         * 启动
         */
        start,
        stop,
        restart,
        status,
        /**
         * 运行日志
         */
        showlog,
        /**
         * 获取进程id
         */
        pid,
        /**
         * 备份日志
         */
        backupLog,
        /**
         * 查看内存信息
         */
        top
    }

//
//    public File getCommandFile() {
//        File file = new File(getRunCommandPath());
//        if (!file.exists()) {
//            throw new RuntimeException("启动文件不存在");
//        }
//        return file;
//    }


    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel) throws ConfigException {
        return execCommand(commandOp, projectInfoModel, null);
    }

    /**
     * 执行shell命令
     *
     * @param commandOp        执行的操作
     * @param projectInfoModel 项目信息
     */
    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel, Evt evt) throws ConfigException {
        String result;

        String commandPath = ConfigBean.getInstance().getRunCommandPath();
        String tag = null, log = null;
        // 项目启动信息
        if (projectInfoModel != null) {
            tag = projectInfoModel.getId();
            log = projectInfoModel.getLog();

        }
        // 执行命令
        String command;
        switch (commandOp) {
            case restart:
            case start: {
                String mainClass = null, lib = null, token = null, jvm = null, args = null;
                if (projectInfoModel != null) {
                    mainClass = projectInfoModel.getMainClass();
                    lib = projectInfoModel.getLib();
                    token = projectInfoModel.getToken();
                    jvm = projectInfoModel.getJvm();
                    args = projectInfoModel.getArgs();
                }
                command = String.format("%s %s %s %s %s %s %s [%s][%s]", commandPath, commandOp.toString(), tag, token, mainClass, lib, log, jvm, args);
                break;
            }
            case stop: {
                String token = null;
                if (projectInfoModel != null) {
                    token = projectInfoModel.getToken();
                }
                command = String.format("%s %s %s %s", commandPath, commandOp.toString(), tag, token);
                break;
            }
            case status:
            case pid:
                command = String.format("%s %s %s", commandPath, commandOp.toString(), tag);
                break;
            case backupLog: {
                command = String.format("%s %s %s", commandPath, commandOp.toString(), log);
                break;
            }
            case top:
                command = "top -b -n 1";
                break;
            case showlog:
            default:
                throw new IllegalArgumentException(commandOp + " error");
        }
        result = execCommand(command, evt);
        //  通知日志刷新
        if (commandOp == CommandOp.start || commandOp == CommandOp.restart) {
            if (projectInfoModel != null) {
                TailLogThread.logChange(log);
                // 修改 run lib 使用情况
                ProjectInfoModel modify = new ProjectInfoModel();
                modify.setId(projectInfoModel.getId());
                modify.setRunLibDesc(projectInfoModel.getUseLibDesc());
                try {
                    projectInfoService.updateProject(modify);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private String execCommand(String command, Evt evt) {
        String result = "error";
        try {
            result = exec(new String[]{command});
        } catch (IOException | InterruptedException e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            if (evt != null) {
                evt.commandError(e);
            }
            result += e.getMessage();
        }
        return result;
    }

    public String execSystemCommand(String command) {
        String result = "error";
        try {
            //执行linux系统命令
            String[] cmd = {"/bin/sh", "-c", command};
            result = exec(cmd);
        } catch (IOException | InterruptedException e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            result += e.getMessage();
        }
        return result;
    }

    private String exec(String[] cmd) throws IOException, InterruptedException {
        DefaultSystemLog.LOG().info(Arrays.toString(cmd));
        String result;
        Process process;
        if (cmd.length == 1) {
            process = Runtime.getRuntime().exec(cmd[0]);
        } else {
            process = Runtime.getRuntime().exec(cmd);
        }
        InputStream is;
        int wait = process.waitFor();
        if (wait == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }
        result = IoUtil.read(is, CharsetUtil.CHARSET_UTF_8);
        is.close();
        process.destroy();
        if (StrUtil.isEmpty(result)) {
            result = "没有返回任何执行信息";
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
                try {
                    socketSession.sendMsg("执行命令异常：" + ExceptionUtil.stacktraceToString(e));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


    public String execCommand(String command) {
        return execCommand(command, null);
    }


}
