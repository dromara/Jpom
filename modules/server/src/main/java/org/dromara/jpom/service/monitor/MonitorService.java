/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.monitor;

import cn.keepbx.jpom.cron.ICron;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.model.data.MonitorModel;
import org.dromara.jpom.monitor.MonitorItem;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.util.StringUtil;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
@Slf4j
public class MonitorService extends BaseWorkspaceService<MonitorModel> implements ICron<MonitorModel> {

    @Override
    public int insert(MonitorModel monitorModel) {
        int count = super.insert(monitorModel);
        this.checkCron(monitorModel);
        return count;
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        int i = super.delByKey(keyValue, request);
        if (i > 0) {
            String taskId = "monitor:" + keyValue;
            CronUtils.remove(taskId);
        }
        return i;
    }

    @Override
    public int updateById(MonitorModel info, HttpServletRequest request) {
        int update = super.updateById(info, request);
        if (update > 0) {
            this.checkCron(info);
        }
        return update;
    }

    @Override
    public List<MonitorModel> queryStartingList() {
        // 关闭监听
        MonitorModel monitorModel = new MonitorModel();
        monitorModel.setStatus(true);
        return super.listByBean(monitorModel);
    }

    /**
     * 检查定时任务 状态
     *
     * @param monitorModel 监控信息
     */
    @Override
    public boolean checkCron(MonitorModel monitorModel) {
        String id = monitorModel.getId();
        String taskId = "monitor:" + id;
        String autoExecCron = monitorModel.getExecCron();
        autoExecCron = StringUtil.parseCron(autoExecCron);
        if (!monitorModel.status(autoExecCron)) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start monitor cron {} {} {}", id, monitorModel.getName(), autoExecCron);
        CronUtils.upsert(taskId, autoExecCron, new MonitorItem(id));
        return true;
    }

    /**
     * 设置报警状态
     *
     * @param id    监控id
     * @param alarm 状态
     */
    public void setAlarm(String id, boolean alarm) {
        MonitorModel monitorModel = new MonitorModel();
        monitorModel.setId(id);
        monitorModel.setAlarm(alarm);
        super.updateById(monitorModel);
    }

    /**
     * 判断是否存在对应节点数据
     *
     * @param nodeId 节点id
     * @return true 存在
     */
    public boolean checkNode(String nodeId) {
        List<MonitorModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (MonitorModel monitorModel : list) {
            List<MonitorModel.NodeProject> projects = monitorModel.projects();
            if (projects != null) {
                for (MonitorModel.NodeProject project : projects) {
                    if (nodeId.equals(project.getNode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /*public boolean checkProject(String nodeId, String projectId) {
        List<MonitorModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (MonitorModel monitorModel : list) {
            List<MonitorModel.NodeProject> projects = monitorModel.projects();
            if (projects != null) {
                for (MonitorModel.NodeProject project : projects) {
                    if (project.getNode().equals(nodeId)) {
                        List<String> projects1 = project.getProjects();
                        if (projects1 != null && projects1.contains(projectId)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }*/
}
