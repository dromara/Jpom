/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.service.monitor;

import cn.hutool.core.date.DateUtil;
import io.jpom.common.BaseOperService;
import io.jpom.model.Cycle;
import io.jpom.model.data.MonitorModel;
import io.jpom.monitor.Monitor;
import io.jpom.system.ServerConfigBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
public class MonitorService extends BaseOperService<MonitorModel> {

    public MonitorService() {
        super(ServerConfigBean.MONITOR_FILE);
    }

    @Override
    public void addItem(MonitorModel monitorModel) {
        super.addItem(monitorModel);
        //
        if (monitorModel.isStatus()) {
            Monitor.start();
        }
    }

    @Override
    public void deleteItem(String id) {
        super.deleteItem(id);
        this.checkCronStatus();
    }

    public boolean checkCronStatus() {
        // 关闭监听
        List<MonitorModel> list = list();
        if (list == null || list.isEmpty()) {
            Monitor.stop();
            return false;
        } else {
            boolean stop = true;
            for (MonitorModel monitorModel : list) {
                if (monitorModel.isStatus()) {
                    Monitor.start();
                    stop = false;
                    break;
                }
            }
            if (stop) {
                Monitor.stop();
                return false;
            }
            return true;
        }
    }

    @Override
    public void updateItem(MonitorModel monitorModel) {
        monitorModel.setModifyTime(DateUtil.date().getTime());
        super.updateItem(monitorModel);
        this.checkCronStatus();
    }

    /**
     * 根据周期获取list
     *
     * @param cycle 周期
     * @return list
     */
    public List<MonitorModel> listRunByCycle(Cycle cycle) {
        List<MonitorModel> list = this.list();
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(monitorModel -> monitorModel.getCycle() == cycle.getCode() && monitorModel.isStatus())
                .collect(Collectors.toList());
    }

    /**
     * 设置报警状态
     *
     * @param id    监控id
     * @param alarm 状态
     */
    public synchronized void setAlarm(String id, boolean alarm) {
        MonitorModel monitorModel = getItem(id);
        if (monitorModel != null) {
            monitorModel.setAlarm(alarm);
            updateItem(monitorModel);
        }
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
            List<MonitorModel.NodeProject> projects = monitorModel.getProjects();
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


    public boolean checkProject(String nodeId, String projectId) {
        List<MonitorModel> list = list();
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (MonitorModel monitorModel : list) {
            List<MonitorModel.NodeProject> projects = monitorModel.getProjects();
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
    }
}
