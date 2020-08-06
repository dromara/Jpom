package io.jpom.model.data;

import io.jpom.model.BaseModel;
import io.jpom.model.log.UserOperateLogV1;

import java.util.List;

/**
 * 监控用户操作实体
 *
 * @author Arno
 */
public class MonitorUserOptModel extends BaseModel {
    /**
     * 监控的人员
     */
    private List<String> monitorUser;

    /**
     * 监控的操作
     */
    private List<UserOperateLogV1.OptType> monitorOpt;
    /**
     * 报警联系人
     */
    private List<String> notifyUser;

    /**
     * 创建人
     */
    private String parent;
    /**
     * 最后修改时间
     */
    private long modifyTime;
    /**
     * 监控开启状态
     */
    private boolean status;

    public List<String> getMonitorUser() {
        return monitorUser;
    }

    public void setMonitorUser(List<String> monitorUser) {
        this.monitorUser = monitorUser;
    }

    public List<UserOperateLogV1.OptType> getMonitorOpt() {
        return monitorOpt;
    }

    public void setMonitorOpt(List<UserOperateLogV1.OptType> monitorOpt) {
        this.monitorOpt = monitorOpt;
    }

    public List<String> getNotifyUser() {
        return notifyUser;
    }

    public void setNotifyUser(List<String> notifyUser) {
        this.notifyUser = notifyUser;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(long modifyTime) {
        this.modifyTime = modifyTime;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
