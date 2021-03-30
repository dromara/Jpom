package io.jpom.system.init;

import ch.qos.logback.core.util.FileSize;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.system.AgentExtConfigBean;
import io.jpom.util.CronUtils;

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

    private static FileSize MAX_SIZE;

    @PreLoadMethod
    private static void startAutoBackLog() {
        if (projectInfoService == null) {
            projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        }
        // 获取cron 表达式
        String cron = StrUtil.emptyToDefault(AgentExtConfigBean.getInstance().autoBackConsoleCron, "none");
        if ("none".equalsIgnoreCase(cron.trim())) {
            //DefaultSystemLog.getLog().info("没有配置自动备份控制台日志表达式");
            //return;
            cron = "0 0/10 * * * ?";
        }
        String size = StrUtil.emptyToDefault(AgentExtConfigBean.getInstance().autoBackSize, "50MB");
        MAX_SIZE = FileSize.valueOf(size.trim());
        //
        CronUtil.schedule(ID, cron, () -> {
            try {
                List<ProjectInfoModel> list = projectInfoService.list();
                if (list == null) {
                    return;
                }
                list.forEach(projectInfoModel -> {
                    checkProject(projectInfoModel, null);
                    //
                    List<ProjectInfoModel.JavaCopyItem> javaCopyItemList = projectInfoModel.getJavaCopyItemList();
                    if (javaCopyItemList == null) {
                        return;
                    }
                    javaCopyItemList.forEach(javaCopyItem -> checkProject(projectInfoModel, javaCopyItem));
                });
            } catch (Exception e) {
                DefaultSystemLog.getLog().error("定时备份日志失败", e);
            }
        });
        CronUtils.start();
    }

    private static void checkProject(ProjectInfoModel projectInfoModel, ProjectInfoModel.JavaCopyItem javaCopyItem) {
        File file = javaCopyItem == null ? new File(projectInfoModel.getLog()) : projectInfoModel.getLog(javaCopyItem);
        if (!file.exists()) {
            return;
        }
        long len = file.length();
        if (len > MAX_SIZE.getSize()) {
            try {
                AbstractProjectCommander.getInstance().backLog(projectInfoModel, javaCopyItem);
            } catch (Exception ignored) {
            }
        }
        // 清理过期的文件
        File logFile = javaCopyItem == null ? projectInfoModel.getLogBack() : projectInfoModel.getLogBack(javaCopyItem);
        DateTime nowTime = DateTime.now();
        List<File> files = FileUtil.loopFiles(logFile, pathname -> {
            DateTime dateTime = DateUtil.date(pathname.lastModified());
            long days = DateUtil.betweenDay(dateTime, nowTime, false);
            long saveDays = AgentExtConfigBean.getInstance().getLogSaveDays();
            return days > saveDays;
        });
        files.forEach(FileUtil::del);
    }
}
