package cn.keepbx.jpom.service.manage;

import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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


    /**
     * 执行shell命令
     *
     * @param commandOp        执行的操作
     * @param projectInfoModel 项目信息
     */
    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel) throws Exception {
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
            case top:
            case showlog:
            default:
                throw new IllegalArgumentException(commandOp + " error");
        }
        //  通知日志刷新
        if (commandOp == CommandOp.start || commandOp == CommandOp.restart) {
            if (projectInfoModel != null) {
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
}
