package cn.keepbx.monitor;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.db.sql.Wrapper;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.model.data.MonitorNotifyLog;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.monitor.MonitorService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.util.CronUtils;
import com.alibaba.fastjson.JSONObject;

import java.sql.SQLException;
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
            String title = null;
            String context = null;
            // 当前状态
            boolean runStatus = false;
            try {
                //查询项目运行状态
                JsonMessage jsonMessage = NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectStatus, JsonMessage.class, "id", id);
                if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                    JSONObject jsonObject = jsonMessage.dataToObj(JSONObject.class);
                    int pid = jsonObject.getIntValue("pId");
                    if (pid > 0) {
                        // 正常运行
                        runStatus = true;
                    } else {
                        //
                        if (monitorModel.isAutoRestart()) {
                            // 执行重启
                            try {
                                JsonMessage reJson = NodeForward.requestData(nodeModel, NodeUrl.Manage_Restart, JsonMessage.class, "id", id);
                                title = StrUtil.format("【{}】节点的【{}】项目已经停止，已经执行重启操作", nodeModel.getName(), id);
                                context = reJson.toString();
                            } catch (Exception e) {
                                DefaultSystemLog.ERROR().error("执行重启操作", e);
                                title = StrUtil.format("【{}】节点的【{}】项目已经停止，重启操作异常", nodeModel.getName(), id);
                                context = ExceptionUtil.stacktraceToString(e);
                            }
                        } else {
                            title = StrUtil.format("【{}】节点的【{}】项目已经没有运行", nodeModel.getName(), id);
                            context = "请及时检查";
                        }
                    }
                } else {
                    title = StrUtil.format("【{}】节点的状态码异常：{}", nodeModel.getName(), jsonMessage.getCode());
                    context = jsonMessage.toString();
                }
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("节点异常", e);
                //
                title = StrUtil.format("【{}】节点的运行状态异常", nodeModel.getName());
                context = ExceptionUtil.stacktraceToString(e);
            }
            //
            boolean pre = getPre(nodeModel.getId(), id);
            MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
            if (!pre) {
                if (runStatus) {
                    title = StrUtil.format("【{}】节点的【{}】项目已经恢复正常运行", nodeModel.getName(), id);
                    context = "";
                } else {
                    // 上一次也是异常，并且当前也是异常
                    return;
                }
            }
            monitorNotifyLog.setStatus(runStatus);
            monitorNotifyLog.setTitle(title);
            monitorNotifyLog.setContent(context);
            monitorNotifyLog.setCreateTime(System.currentTimeMillis());
            monitorNotifyLog.setNodeId(nodeModel.getId());
            monitorNotifyLog.setProjectId(id);
            //
            List<MonitorModel.Notify> notify = monitorModel.getNotify();
            this.notifyMsg(notify, title, context, monitorNotifyLog);
        });
    }

    private boolean getPre(String nodeId, String projectId) {
        // 检查是否已经触发通知
        Entity entity = Entity.create(MonitorNotifyLog.TABLE_NAME);
        entity.set("nodeId", nodeId);
        entity.set("projectId", projectId);
        entity.set("status", false);
        Page page = new Page(0, 1);
        page.addOrder(new Order("createTime", Direction.DESC));
        PageResult<Entity> pageResult;
        try {
            pageResult = Db.use().setWrapper((Character) null).page(entity, page);
            return pageResult.isEmpty();
        } catch (SQLException e) {
            DefaultSystemLog.ERROR().error("数据库查询异常", e);
            // 如果异常默认上次是正常状态
            return true;
        }
    }

    private void notifyMsg(List<MonitorModel.Notify> notify, String title, String context, MonitorNotifyLog monitorNotifyLog) {
        if (title != null) {
            notify.forEach(notify1 -> {
                monitorNotifyLog.setLogId(IdUtil.fastSimpleUUID());
                monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                insert(monitorNotifyLog);
                NotifyUtil.send(notify1, title, context);
            });
        }
    }

    private void insert(MonitorNotifyLog monitorNotifyLog) {
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            Entity entity = new Entity(MonitorNotifyLog.class.getSimpleName().toUpperCase());
            entity.parseBean(monitorNotifyLog);
            db.insert(entity);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
