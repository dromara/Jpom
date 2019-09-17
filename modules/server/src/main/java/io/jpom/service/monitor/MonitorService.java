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
