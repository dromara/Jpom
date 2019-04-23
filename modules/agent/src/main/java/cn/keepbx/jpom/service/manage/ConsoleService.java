package cn.keepbx.jpom.service.manage;

import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.socket.CommandOp;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 控制台
 * Created by jiangzeyin on 2018/9/28.
 *
 * @author jiangzeyin
 */
@Service
public class ConsoleService {


    @Resource
    private ProjectInfoService projectInfoService;


    /**
     * 执行shell命令
     *
     * @param commandOp        执行的操作
     * @param projectInfoModel 项目信息
     */
    public String execCommand(CommandOp commandOp, ProjectInfoModel projectInfoModel) throws Exception {
        String result;
        AbstractProjectCommander abstractProjectCommander = AbstractProjectCommander.getInstance();
        // 执行命令
        switch (commandOp) {
            case restart:
                result = abstractProjectCommander.restart(projectInfoModel);
                break;
            case start:
                result = abstractProjectCommander.start(projectInfoModel);
                break;
            case stop:
                result = abstractProjectCommander.stop(projectInfoModel);
                break;
            case status: {
                String tag = projectInfoModel.getId();
                result = abstractProjectCommander.status(tag);
                break;
            }
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
                    projectInfoService.updateItem(modify);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }
}
