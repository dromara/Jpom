package cn.keepbx.monitor;

import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.service.monitor.MonitorService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.util.CronUtils;

import java.util.List;

/**
 * 监听调度
 *
 * @author bwcx_jzy
 * @date 2019/7/12
 **/
public class Monitor implements Task {

    private static final String CRON_ID = "Monitor";

    /**
     * 开启调度
     */
    public static void start() {
        Task task = CronUtil.getScheduler().getTask(CRON_ID);
        if (task == null) {
            CronUtil.schedule(CRON_ID, MonitorModel.Cycle.one.getCronPattern().toString(), new Monitor());
            CronUtils.start();
        }
    }

    public static void stop() {
        CronUtil.remove(CRON_ID);
    }

    @Override
    public void execute() {
        long time = System.currentTimeMillis();
        MonitorService monitorService = SpringUtil.getBean(MonitorService.class);
        //
        List<MonitorModel> monitorModels = monitorService.listRunByCycle(MonitorModel.Cycle.one);
        //
        if (MonitorModel.Cycle.five.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
            monitorModels.addAll(monitorService.listRunByCycle(MonitorModel.Cycle.five));
        }
        //
        if (MonitorModel.Cycle.ten.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
            monitorModels.addAll(monitorService.listRunByCycle(MonitorModel.Cycle.ten));
        }
        //
        if (MonitorModel.Cycle.thirty.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
            monitorModels.addAll(monitorService.listRunByCycle(MonitorModel.Cycle.thirty));
        }
        //
        this.checkList(monitorModels);
    }

    private void checkList(List<MonitorModel> monitorModels) {
        if (monitorModels == null || monitorModels.isEmpty()) {
            return;
        }
        monitorModels.forEach(monitorModel -> {
            List<MonitorModel.NodeProject> nodeProjects = monitorModel.getProjects();
            List<MonitorModel.Notify> notifies = monitorModel.getNotify();
            if (notifies == null || notifies.isEmpty()) {
                return;
            }
            this.checkNode(nodeProjects, notifies);
        });
    }

    private void checkNode(List<MonitorModel.NodeProject> nodeProjects, List<MonitorModel.Notify> notifies) {
        if (nodeProjects == null || nodeProjects.isEmpty()) {
            return;
        }
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        nodeProjects.forEach(nodeProject -> {
            String nodeId = nodeProject.getNode();
            NodeModel nodeModel = nodeService.getItem(nodeId);
            if (nodeModel == null) {
                return;
            }
            this.reqNodeStatus(nodeModel, nodeProject.getProjects(), notifies);
        });
    }

    private void reqNodeStatus(NodeModel nodeModel, List<String> projects, List<MonitorModel.Notify> notifies) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        projects.forEach(id -> {
            try {
                //查询项目运行状态
                JsonMessage jsonMessage = NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectStatus, JsonMessage.class, "id", id);
            } catch (Exception e) {
                //

            }
        });
    }

    private void notifyMsg() {

    }
}
