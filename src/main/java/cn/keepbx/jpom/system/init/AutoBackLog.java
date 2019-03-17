package cn.keepbx.jpom.system.init;

import ch.qos.logback.core.util.FileSize;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.Scheduler;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.commander.AbstractCommander;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;

import java.io.File;
import java.util.List;

/**
 * 自动备份控制台日志，防止日志文件过大
 *
 * @author jiangzeyin
 * @date 2019/3/17
 */
@PreLoadClass
public class AutoBackLog {

    private static final String ID = "auto_back_log";
    private static ProjectInfoService projectInfoService;

    private static final FileSize MAX_SIZE = FileSize.valueOf("50MB");

    @PreLoadMethod
    private static void startAutoBackLog() {
        CronUtil.setMatchSecond(true);
        if (projectInfoService == null) {
            projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        }
        //
        CronUtil.schedule(ID, "0 0/10 * * * ?", () -> {
            try {
                List<ProjectInfoModel> list = projectInfoService.list();
                if (list == null) {
                    return;
                }
                list.forEach(projectInfoModel -> {
                    String log = projectInfoModel.getLog();
                    File file = new File(log);
                    if (!file.exists()) {
                        return;
                    }
                    long len = file.length();
                    if (len > MAX_SIZE.getSize()) {
                        try {
                            AbstractCommander.getInstance().backLog(projectInfoModel);
                        } catch (Exception ignored) {
                        }
                    }
                });
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("定时备份日志失败", e);
            }
        });
        Scheduler scheduler = CronUtil.getScheduler();
        if (!scheduler.isStarted()) {
            CronUtil.start();
        }
    }
}
