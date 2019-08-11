package cn.keepbx.monitor;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.http.HttpStatus;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.MonitorModel;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.MonitorNotifyLog;
import cn.keepbx.jpom.service.dblog.DbMonitorNotifyLogService;
import cn.keepbx.jpom.service.monitor.MonitorService;
import cn.keepbx.jpom.service.node.NodeService;
import cn.keepbx.jpom.service.user.UserService;
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


    private static DbMonitorNotifyLogService dbMonitorNotifyLogService;


    /**
     * 开启调度
     */
    public static void start() {
        Task task = CronUtil.getScheduler().getTask(CRON_ID);
        if (task == null) {
            CronUtil.schedule(CRON_ID, MonitorModel.Cycle.one.getCronPattern().toString(), new Monitor());
            CronUtils.start();
        }
        dbMonitorNotifyLogService = SpringUtil.getBean(DbMonitorNotifyLogService.class);
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
            List<String> notifyUser = monitorModel.getNotifyUser();
            if (notifyUser == null || notifyUser.isEmpty()) {
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
            // 获取上次状态
            boolean pre = getPreStatus(monitorModel.getId(), nodeModel.getId(), id);
            String title = null;
            String context = null;
            // 当前状态
            boolean runStatus = false;
            try {
                //查询项目运行状态
                JsonMessage jsonMessage = NodeForward.requestBySys(nodeModel, NodeUrl.Manage_GetProjectStatus, "id", id);
                if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
                    JSONObject jsonObject = jsonMessage.dataToObj(JSONObject.class);
                    int pid = jsonObject.getIntValue("pId");
                    if (pid > 0) {
                        // 正常运行
                        runStatus = true;
                        if (!pre) {
                            // 上次是异常状态
                            title = StrUtil.format("【{}】节点的【{}】项目已经恢复正常运行", nodeModel.getName(), id);
                            context = "";
                        }
                    } else {
                        //
                        if (monitorModel.isAutoRestart()) {
                            // 执行重启
                            try {
                                JsonMessage reJson = NodeForward.requestBySys(nodeModel, NodeUrl.Manage_Restart, "id", id);
                                if (reJson.getCode() == HttpStatus.HTTP_OK) {
                                    // 重启成功
                                    runStatus = true;
                                    title = StrUtil.format("【{}】节点的【{}】项目已经停止，已经执行重启操作,结果成功", nodeModel.getName(), id);
                                } else {
                                    title = StrUtil.format("【{}】节点的【{}】项目已经停止，已经执行重启操作,结果失败", nodeModel.getName(), id);
                                }
                                context = "重启结果：" + reJson.toString();
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
            if (!pre && !runStatus) {
                // 上一次也是异常，并且当前也是异常
                return;
            }
            MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
            monitorNotifyLog.setStatus(runStatus);
            monitorNotifyLog.setTitle(title);
            monitorNotifyLog.setContent(context);
            monitorNotifyLog.setCreateTime(System.currentTimeMillis());
            monitorNotifyLog.setNodeId(nodeModel.getId());
            monitorNotifyLog.setProjectId(id);
            monitorNotifyLog.setMonitorId(monitorModel.getId());
            //
            List<String> notify = monitorModel.getNotifyUser();
            this.notifyMsg(notify, monitorNotifyLog);
        });
    }

    /**
     * 获取上次是否也为异常状态
     *
     * @param monitorId 监控id
     * @param nodeId    节点id
     * @param projectId 项目id
     * @return true 为正常状态,false 异常状态
     */
    private boolean getPreStatus(String monitorId, String nodeId, String projectId) {
        // 检查是否已经触发通知
        Entity entity = Entity.create();
        entity.set("nodeId", nodeId);
        entity.set("projectId", projectId);
        entity.set("monitorId", monitorId);
        Page page = new Page(0, 1);
        page.addOrder(new Order("createTime", Direction.DESC));


        PageResult<MonitorNotifyLog> pageResult = dbMonitorNotifyLogService.listPage(entity, page);
        if (pageResult.isEmpty()) {
            return true;
        }
        MonitorNotifyLog entity1 = pageResult.get(0);
        return entity1.isStatus();
    }

    private void notifyMsg(final List<String> notify, final MonitorNotifyLog monitorNotifyLog) {
        // 报警状态
        MonitorService monitorService = SpringUtil.getBean(MonitorService.class);
        monitorService.setAlarm(monitorNotifyLog.getMonitorId(), !monitorNotifyLog.isStatus());
        UserService userService = SpringUtil.getBean(UserService.class);
        // 发送通知
        if (monitorNotifyLog.getTitle() != null) {
            notify.forEach(notifyUser -> {
                UserModel item = userService.getItem(notifyUser);
                boolean success = false;
                if (item != null) {
                    // 邮箱
                    String email = item.getEmail();
                    if (StrUtil.isNotEmpty(email)) {
                        monitorNotifyLog.setLogId(IdUtil.fastSimpleUUID());
                        MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
                        monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                        monitorNotifyLog.setNotifyObject(notify1.getValue());
                        //
                        dbMonitorNotifyLogService.insert(monitorNotifyLog);
                        send(notify1, monitorNotifyLog.getLogId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                        success = true;
                    }
                    // dingding
                    String dingDing = item.getDingDing();
                    if (StrUtil.isNotEmpty(dingDing)) {
                        monitorNotifyLog.setLogId(IdUtil.fastSimpleUUID());
                        MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
                        monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                        monitorNotifyLog.setNotifyObject(notify1.getValue());
                        //
                        dbMonitorNotifyLogService.insert(monitorNotifyLog);
                        send(notify1, monitorNotifyLog.getLogId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                        success = true;
                    }
                }
                if (success) {
                    return;
                }
                monitorNotifyLog.setLogId(IdUtil.fastSimpleUUID());
                monitorNotifyLog.setNotifyObject("报警联系人异常");
                monitorNotifyLog.setNotifyStyle(MonitorModel.NotifyType.mail.getCode());
                monitorNotifyLog.setNotifyStatus(false);
                monitorNotifyLog.setNotifyError("报警联系人异常:" + (item == null ? "联系人不存在" : ""));
                dbMonitorNotifyLogService.insert(monitorNotifyLog);
            });
        }
    }

    private void send(MonitorModel.Notify notify, String logId, String title, String context) {
        // 异常发送
        ThreadUtil.execute(() -> {
            try {
                NotifyUtil.send(notify, title, context);
                dbMonitorNotifyLogService.updateStatus(logId, true, null);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("发送报警通知异常", e);
                dbMonitorNotifyLogService.updateStatus(logId, false, ExceptionUtil.stacktraceToString(e));
            }
        });
    }
}
