package cn.jiangzeyin.service.manage;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.jiangzeyin.model.ProjectInfoModel;
import cn.jiangzeyin.service.BaseService;
import cn.jiangzeyin.socket.LogWebSocketHandle;
import cn.jiangzeyin.socket.SocketSession;
import cn.jiangzeyin.socket.TailLogThread;
import cn.jiangzeyin.socket.top.TopManager;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jiangzeyin on 2018/9/28.
 *
 * @author jiangzeyin
 */
@Service
public class CommandService extends BaseService {
    public static final String RUNING_TAG = "running";
    public static final String STOP_TAG = "stopped";

    @Resource
    private ManageService manageService;


    public enum CommandOp {
        /**
         * 启动
         */
        start,
        stop,
        restart,
        status,

        showlog,
        /**
         * 获取进程id
         */
        getPid,
        /**
         * 备份日志
         */
        backupLog,
        top
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

    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel) {
        return execCommand(commandOp, projectInfoModel, null);
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
        String tag = null, mainClass = null, lib = null, log = null, token = null, jvm = null, args = null;
        // 项目启动信息
        if (projectInfoModel != null) {
            tag = projectInfoModel.getTag();
            mainClass = projectInfoModel.getMainClass();
            lib = projectInfoModel.getLib();
            log = projectInfoModel.getLog();
            token = projectInfoModel.getToken();
            jvm = projectInfoModel.getJvm();
            args = projectInfoModel.getArgs();
        }
        // 执行命令
        String command;
        switch (commandOp) {
            case restart:
            case start:
            case status:
                command = String.format("%s %s %s %s %s %s %s [%s][%s]", commandPath, commandOp.toString(), tag, mainClass, lib, log, token, jvm, args);
                break;
            case stop:
                command = String.format("%s %s %s %s", commandPath, commandOp.toString(), tag, token);
                break;
            case getPid:
                command = String.format("%s %s %s", commandPath, commandOp.toString(), tag);
                break;
            case backupLog:
                command = String.format("%s %s %s", commandPath, commandOp.toString(), log);
                break;
            case top:
                String savePath = TopManager.getTopFile();
                command = String.format("top -b -d 1 -n 1 > %s", savePath);
                break;
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
                    manageService.updateProject(modify);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private String execCommand(String command, Evt evt) {
        String result = "error";
        try {
            DefaultSystemLog.LOG().info(command);
            Process process = Runtime.getRuntime().exec(command);
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
        } catch (IOException | InterruptedException e) {
            DefaultSystemLog.ERROR().error("执行命令异常", e);
            if (evt != null) {
                evt.commandError(e);
            }
            result += e.getMessage();
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

    /**
     * 获取内存信息
     *
     * @param tag tag
     * @return 内存信息
     */
    public JSONObject getInternal(String tag) {
        String pid = getPid(tag);
        String topRam = getTopRam(pid);
        System.out.println(pid);
        System.out.println(topRam);
        return null;
    }

    private String getTopRam(String pid) {
        String command = "top -p " + pid;
        return execCommand(command, null);
    }

    /**
     * 获取进程pid
     *
     * @param tag tag
     * @return pid
     */
    private String getPid(String tag) {
        String commandPath = getCommandPath();
        String command = String.format("%s %s %s", commandPath, "pid", tag);
        return execCommand(command, null);
    }

}
