/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.monitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.MonitorModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.log.MonitorNotifyLog;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.dblog.DbMonitorNotifyLogService;
import org.dromara.jpom.service.monitor.MonitorService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.user.UserService;
import org.dromara.jpom.webhook.DefaultWebhookPluginImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控执行器
 *
 * @author bwcx_jzy
 * @since 2021/12/14
 */
@Slf4j
public class MonitorItem implements Task {


    private final DbMonitorNotifyLogService dbMonitorNotifyLogService;
    private final UserService userService;
    private final MonitorService monitorService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final NodeService nodeService;
    private final String monitorId;
    private MonitorModel monitorModel;

    public MonitorItem(String id) {
        this.dbMonitorNotifyLogService = SpringUtil.getBean(DbMonitorNotifyLogService.class);
        this.userService = SpringUtil.getBean(UserService.class);
        this.monitorService = SpringUtil.getBean(MonitorService.class);
        this.nodeService = SpringUtil.getBean(NodeService.class);
        this.projectInfoCacheService = SpringUtil.getBean(ProjectInfoCacheService.class);
        this.monitorId = id;
    }

    @Override
    public void execute() {
        // 重新查询
        this.monitorModel = monitorService.getByKey(monitorId);
        List<MonitorModel.NodeProject> nodeProjects = monitorModel.projects();
        //
        List<Boolean> collect = nodeProjects.stream().map(nodeProject -> {
            String nodeId = nodeProject.getNode();
            NodeModel nodeModel = nodeService.getByKey(nodeId);
            if (nodeModel == null) {
                return true;
            }
            return this.reqNodeStatus(nodeModel, nodeProject.getProjects());
        }).filter(aBoolean -> !aBoolean).collect(Collectors.toList());
        boolean allRun = CollUtil.isEmpty(collect);
        // 报警状态
        monitorService.setAlarm(monitorModel.getId(), !allRun);
    }

    /**
     * 检查节点节点对信息
     *
     * @param nodeModel 节点
     * @param projects  项目
     * @return true 所有项目都正常
     */
    private boolean reqNodeStatus(NodeModel nodeModel, List<String> projects) {
        if (projects == null || projects.isEmpty()) {
            return true;
        }
        List<Boolean> collect = projects.stream().map(id -> {
            //
            String title;
            String context;
            try {
                //查询项目运行状态
                JsonMessage<JSONObject> jsonMessage = NodeForward.request(nodeModel, NodeUrl.Manage_GetProjectStatus, "id", id);
                if (jsonMessage.success()) {
                    JSONObject jsonObject = jsonMessage.getData();
                    int pid = jsonObject.getIntValue("pId");
                    String statusMsg = jsonObject.getString("statusMsg");
                    boolean runStatus = this.checkNotify(monitorModel, nodeModel, id, pid > 0, statusMsg);
                    // 检查副本
                    List<Boolean> booleanList = null;
                    JSONArray copys = jsonObject.getJSONArray("copys");
                    if (CollUtil.isNotEmpty(copys)) {
                        booleanList = copys.stream()
                            .map(o -> {
                                JSONObject jsonObject1 = (JSONObject) o;

                                boolean status = jsonObject1.getBooleanValue("status");
                                return MonitorItem.this.checkNotify(monitorModel, nodeModel, id, status, StrUtil.EMPTY);
                            })
                            .filter(aBoolean -> !aBoolean)
                            .collect(Collectors.toList());
                    }
                    return runStatus && CollUtil.isEmpty(booleanList);
                } else {
                    title = StrUtil.format(I18nMessageUtil.get("i18n.node_status_code_abnormal.4d22"), nodeModel.getName(), jsonMessage.getCode());
                    context = jsonMessage.toString();
                }
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.monitor_node_exception.6ff1"), nodeModel.getName(), e.getMessage());
                //
                title = StrUtil.format(I18nMessageUtil.get("i18n.node_running_status_abnormal.3160"), nodeModel.getName());
                context = ExceptionUtil.stacktraceToString(e);
            }
            // 获取上次状态
            boolean pre = this.getPreStatus(monitorModel.getId(), nodeModel.getId(), id);
            if (pre) {
                // 上次正常
                MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
                monitorNotifyLog.setStatus(false);
                monitorNotifyLog.setTitle(title);
                monitorNotifyLog.setContent(context);
                monitorNotifyLog.setCreateTime(System.currentTimeMillis());
                monitorNotifyLog.setNodeId(nodeModel.getId());
                monitorNotifyLog.setProjectId(id);
                monitorNotifyLog.setMonitorId(monitorModel.getId());
                //
                this.notifyMsg(nodeModel, monitorNotifyLog);
            }
            return false;
        }).filter(aBoolean -> !aBoolean).collect(Collectors.toList());
        return CollUtil.isEmpty(collect);
    }

    /**
     * 检查状态
     *
     * @param monitorModel 监控信息
     * @param nodeModel    节点信息
     * @param id           项目id
     * @param runStatus    当前运行状态
     */
    private boolean checkNotify(MonitorModel monitorModel, NodeModel nodeModel, String id, boolean runStatus, String statusMsg) {
        // 获取上次状态
        String copyMsg = StrUtil.EMPTY;
        boolean pre = this.getPreStatus(monitorModel.getId(), nodeModel.getId(), id);
        String title = null;
        String context = null;
        //查询项目运行状态
        if (runStatus) {
            if (!pre) {
                // 上次是异常状态
                title = StrUtil.format(I18nMessageUtil.get("i18n.node_service_resumed_normal_operation.2cbd"), nodeModel.getName(), id, copyMsg);
                context = "";
            }
        } else {
            //
            if (monitorModel.autoRestart()) {
                // 执行重启
                try {
                    JsonMessage<String> reJson = NodeForward.request(nodeModel, NodeUrl.Manage_Operate, "id", id, "opt", "restart");
                    if (reJson.success()) {
                        // 重启成功
                        runStatus = true;
                        title = StrUtil.format(I18nMessageUtil.get("i18n.node_service_stopped_successful_restart.603b"), nodeModel.getName(), id, copyMsg);
                    } else {
                        title = StrUtil.format(I18nMessageUtil.get("i18n.node_service_stopped_failed_restart.4307"), nodeModel.getName(), id, copyMsg);
                    }
                    context = I18nMessageUtil.get("i18n.restart_result.253f") + reJson;
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.restart_operation.5e3a"), e);
                    title = StrUtil.format(I18nMessageUtil.get("i18n.node_service_stopped_abnormal_restart.a5c0"), nodeModel.getName(), id, copyMsg);
                    context = ExceptionUtil.stacktraceToString(e);
                }
            } else {
                title = StrUtil.format(I18nMessageUtil.get("i18n.node_service_not_running.ad89"), nodeModel.getName(), id, copyMsg);
                context = I18nMessageUtil.get("i18n.please_check_in_time.3b4f");
            }
        }
        if (!pre && !runStatus) {
            // 上一次是异常，并且当前还是异常
            return false;
        }
        MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
        monitorNotifyLog.setStatus(runStatus);
        monitorNotifyLog.setTitle(title);
        monitorNotifyLog.setContent(StrUtil.format(I18nMessageUtil.get("i18n.alert_content_and_status.6ed1"), context, statusMsg));
        monitorNotifyLog.setCreateTime(System.currentTimeMillis());
        monitorNotifyLog.setNodeId(nodeModel.getId());
        monitorNotifyLog.setProjectId(id);
        monitorNotifyLog.setMonitorId(monitorModel.getId());
        //
        this.notifyMsg(nodeModel, monitorNotifyLog);
        return runStatus;
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

        MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
        monitorNotifyLog.setNodeId(nodeId);
        monitorNotifyLog.setProjectId(projectId);
        monitorNotifyLog.setMonitorId(monitorId);

        List<MonitorNotifyLog> queryList = dbMonitorNotifyLogService.queryList(monitorNotifyLog, 1, new Order("createTime", Direction.DESC));
        MonitorNotifyLog entity1 = CollUtil.getFirst(queryList);
        return entity1 == null || entity1.status();
    }

    private void notifyMsg(NodeModel nodeModel, MonitorNotifyLog monitorNotifyLog) {
        List<String> notify = monitorModel.notifyUser();
        // 发送通知
        if (monitorNotifyLog.getTitle() == null) {
            return;
        }
        ProjectInfoCacheModel projectInfoCacheModel = projectInfoCacheService.getData(nodeModel.getId(), monitorNotifyLog.getProjectId());
        monitorNotifyLog.setWorkspaceId(projectInfoCacheModel.getWorkspaceId());
        //
        notify.forEach(notifyUser -> this.sendNotifyMsgToUser(monitorNotifyLog, notifyUser));
        //
        this.sendNotifyMsgToWebhook(monitorNotifyLog, nodeModel, projectInfoCacheModel, monitorModel.getWebhook());
    }

    private void sendNotifyMsgToWebhook(MonitorNotifyLog monitorNotifyLog, NodeModel nodeModel, ProjectInfoCacheModel projectInfoCacheModel, String webhook) {
        if (StrUtil.isEmpty(webhook)) {
            return;
        }
        IPlugin plugin = PluginFactory.getPlugin("webhook");
        Map<String, Object> map = new HashMap<>(10);
        map.put("JPOM_WEBHOOK_EVENT", DefaultWebhookPluginImpl.WebhookEvent.MONITOR);
        map.put("monitorId", monitorModel.getId());
        map.put("monitorName", monitorModel.getName());
        map.put("nodeId", monitorNotifyLog.getNodeId());
        map.put("nodeName", nodeModel.getName());
        map.put("runStatus", monitorNotifyLog.getStatus());
        map.put("projectId", monitorNotifyLog.getProjectId());
        if (projectInfoCacheModel != null) {
            map.put("projectName", projectInfoCacheModel.getName());
        }
        map.put("title", monitorNotifyLog.getTitle());
        map.put("content", monitorNotifyLog.getContent());
        //
        monitorNotifyLog.setId(IdUtil.fastSimpleUUID());
        monitorNotifyLog.setNotifyStyle(MonitorModel.NotifyType.webhook.getCode());
        monitorNotifyLog.setNotifyObject(webhook);
        //
        dbMonitorNotifyLogService.insert(monitorNotifyLog);
        String logId = monitorNotifyLog.getId();
        ThreadUtil.execute(() -> {
            try {
                plugin.execute(webhook, map);
                dbMonitorNotifyLogService.updateStatus(logId, true, null);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.webhooks_invocation_error.9792"), e);
                dbMonitorNotifyLogService.updateStatus(logId, false, ExceptionUtil.stacktraceToString(e));
            }
        });
    }

    private void sendNotifyMsgToUser(MonitorNotifyLog monitorNotifyLog, String notifyUser) {
        UserModel item = userService.getByKey(notifyUser);
        boolean success = false;
        if (item != null) {
            // 邮箱
            String email = item.getEmail();
            if (StrUtil.isNotEmpty(email)) {
                monitorNotifyLog.setId(IdUtil.fastSimpleUUID());
                MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
                monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                monitorNotifyLog.setNotifyObject(notify1.getValue());
                //
                dbMonitorNotifyLogService.insert(monitorNotifyLog);
                this.send(notify1, monitorNotifyLog.getId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                success = true;
            }
            // dingding
            String dingDing = item.getDingDing();
            if (StrUtil.isNotEmpty(dingDing)) {
                monitorNotifyLog.setId(IdUtil.fastSimpleUUID());
                MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
                monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                monitorNotifyLog.setNotifyObject(notify1.getValue());
                //
                dbMonitorNotifyLogService.insert(monitorNotifyLog);
                this.send(notify1, monitorNotifyLog.getId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                success = true;
            }
            // 企业微信
            String workWx = item.getWorkWx();
            if (StrUtil.isNotEmpty(workWx)) {
                monitorNotifyLog.setId(IdUtil.fastSimpleUUID());
                MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.workWx, workWx);
                monitorNotifyLog.setNotifyStyle(notify1.getStyle());
                monitorNotifyLog.setNotifyObject(notify1.getValue());
                //
                dbMonitorNotifyLogService.insert(monitorNotifyLog);
                this.send(notify1, monitorNotifyLog.getId(), monitorNotifyLog.getTitle(), monitorNotifyLog.getContent());
                success = true;
            }
        }
        if (success) {
            return;
        }
        monitorNotifyLog.setId(IdUtil.fastSimpleUUID());
        monitorNotifyLog.setNotifyObject(I18nMessageUtil.get("i18n.alert_contact_exception.2cec"));
        monitorNotifyLog.setNotifyStyle(MonitorModel.NotifyType.mail.getCode());
        monitorNotifyLog.setNotifyStatus(false);
        String userNotFound = I18nMessageUtil.get("i18n.contact_does_not_exist.3369");
        String notifyError = I18nMessageUtil.get("i18n.alert_contact_exception_message.1072") + (item == null ? userNotFound : "");
        monitorNotifyLog.setNotifyError(notifyError);
        dbMonitorNotifyLogService.insert(monitorNotifyLog);
    }

    private void send(MonitorModel.Notify notify, String logId, String title, String context) {
        // 异常发送
        ThreadUtil.execute(() -> {
            try {
                NotifyUtil.send(notify, title, context);
                dbMonitorNotifyLogService.updateStatus(logId, true, null);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.send_alert_notification_exception.6788"), e);
                dbMonitorNotifyLogService.updateStatus(logId, false, ExceptionUtil.stacktraceToString(e));
            }
        });
    }
}
