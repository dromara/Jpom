package cn.keepbx.monitor;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.service.monitor.MonitorService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.util.CronUtils;
import com.alibaba.fastjson.JSONObject;

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
            if (nodeProjects == null || nodeProjects.isEmpty()) {
                return;
            }
            //
            List<MonitorModel.Notify> notifies = monitorModel.getNotify();
            if (notifies == null || notifies.isEmpty()) {
                return;
            }
            this.checkNode(monitorModel);
        });
    }

    private void checkNode(MonitorModel monitorModel) {
        List<MonitorModel.NodeProject> nodeProjects = monitorModel.getProjects();
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        nodeProjects.forEach(nodeProject -> {
            String nodeId = nodeProject.getNode();
            NodeModel nodeModel = nodeService.getItem(nodeId);
            if (nodeModel == null) {
                return;
            }
            this.reqNodeStatus(monitorModel, nodeModel, nodeProject.getProjects());
        });
    }

    private void reqNodeStatus(MonitorModel monitorModel, NodeModel nodeModel, List<String> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }
        projects.forEach(id -> {
            String title;
            String context;
            try {
                //查询项目运行状态
                JsonMessage jsonMessage = NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectStatus, JsonMessage.class, "id", id);
                if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                    JSONObject jsonObject = jsonMessage.dataToObj(JSONObject.class);
                    int pid = jsonObject.getIntValue("pId");
                    if (pid > 0) {
                        // 正常运行
                        return;
                    } else {
                        //
                        if (monitorModel.isAutoRestart()) {
                            // 执行重启
                        } else {
                            title = StrUtil.format("【{}】节点的【{}】项目已经没有运行", nodeModel.getName(), id);
                            context = "";
                        }
                    }
                } else {
                    title = StrUtil.format("【{}】节点的状态码异常：{}", nodeModel.getName(), jsonMessage.getCode());
                    context = jsonMessage.toString();
                }
            } catch (Exception e) {
                //
                title = StrUtil.format("【{}】节点的运行状态异常", nodeModel.getName());
                context = ExceptionUtil.stacktraceToString(e);
            }
            //

        });
    }

    private void notifyMsg() {

    }
}
