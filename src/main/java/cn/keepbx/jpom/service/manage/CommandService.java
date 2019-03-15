package cn.keepbx.jpom.service.manage;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.socket.LogWebSocketHandle;
import cn.keepbx.jpom.socket.SocketSession;
import cn.keepbx.jpom.socket.TailLogThread;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.websocket.Session;
import java.io.IOException;

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


    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel) throws Exception {
        return execCommand(commandOp, projectInfoModel, null);
    }

    /**
     * 执行shell命令
     *
     * @param commandOp        执行的操作
     * @param projectInfoModel 项目信息
     */
    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel, Evt evt) throws Exception {
        String result = "";
        AbstractCommander abstractCommander = AbstractCommander.getInstance();
        // 执行命令
        switch (commandOp) {
            case restart:
                result = abstractCommander.restart(projectInfoModel);
                break;
            case start:
                result = abstractCommander.start(projectInfoModel);
                break;
            case stop:
                result = abstractCommander.stop(projectInfoModel);
                break;
            case status: {
                String tag = projectInfoModel.getId();
                result = abstractCommander.status(tag);
                break;
            }
            case pid: {
                String tag = projectInfoModel.getId();
                result = abstractCommander.getPid(tag);
                break;
            }
            case backupLog:
                result = abstractCommander.backLog(projectInfoModel);
                break;
            case top: {
                String command = "top -b -n 1";
                return AbstractCommander.getInstance().execCommand(command);
            }
            case showlog:

            default:
                throw new IllegalArgumentException(commandOp + " error");
        }
        //  通知日志刷新
        if (commandOp == CommandOp.start || commandOp == CommandOp.restart) {
            if (projectInfoModel != null) {
                String log = projectInfoModel.getLog();
                TailLogThread.logChange(log);
                // 修改 run lib 使用情况
                ProjectInfoModel modify = projectInfoService.getItem(projectInfoModel.getId());
                modify.setRunLibDesc(projectInfoModel.getUseLibDesc());
                try {
                    projectInfoService.updateProject(modify);
                } catch (Exception ignored) {
                }
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
                try {
                    socketSession.sendMsg("执行命令异常：" + ExceptionUtil.stacktraceToString(e));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


}
