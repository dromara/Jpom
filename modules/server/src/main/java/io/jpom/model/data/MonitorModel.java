package io.jpom.model.data;

import io.jpom.model.BaseEnum;
import io.jpom.model.BaseJsonModel;
import io.jpom.model.BaseModel;
import io.jpom.model.Cycle;

import java.util.List;

/**
 * 监控管理实体
 *
 * @author Arno
 */
public class MonitorModel extends BaseModel {
    /**
     * 监控的项目
     */
    private List<NodeProject> projects;
    /**
     * 报警联系人
     */
    private List<String> notifyUser;
    /**
     * 异常后是否自动重启
     */
    private boolean autoRestart;
    /**
     * 监控周期
     */
    private int cycle = Cycle.thirty.getCode();
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
    /**
     * 报警状态
     */
    private boolean alarm;

    public boolean isAlarm() {
        return alarm;
    }

    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
    }

    public List<NodeProject> getProjects() {
        return projects;
    }

    public void setProjects(List<NodeProject> projects) {
        this.projects = projects;
    }

    public List<String> getNotifyUser() {
        return notifyUser;
    }

    public void setNotifyUser(List<String> notifyUser) {
        this.notifyUser = notifyUser;
    }


    public boolean isAutoRestart() {
        return autoRestart;
    }

    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }

    public int getCycle() {
        return cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
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

    public boolean checkNodeProject(String nodeId, String projectId) {
        List<NodeProject> projects = getProjects();
        if (projects == null) {
            return false;
        }
        for (NodeProject project : projects) {
            if (project.getNode().equals(nodeId)) {
                List<String> projects1 = project.getProjects();
                if (projects1 == null) {
                    return false;
                }
                for (String s : projects1) {
                    if (projectId.equals(s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public enum NotifyType implements BaseEnum {
        /**
         * 通知方式
         */
        dingding(0, "钉钉"),
        mail(1, "邮箱"),
        workWx(2, "企业微信"),
//        sms(2, "短信"),
        ;

        private final int code;
        private final String desc;

        NotifyType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }
    }

    /**
     * 通知
     */
    public static class Notify extends BaseJsonModel {
        private int style;
        private String value;

        public Notify() {
        }

        public Notify(NotifyType style, String value) {
            this.style = style.getCode();
            this.value = value;
        }

        public int getStyle() {
            return style;
        }

        public void setStyle(int style) {
            this.style = style;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class NodeProject extends BaseJsonModel {
        private String node;
        private List<String> projects;

        public String getNode() {
            return node;
        }

        public void setNode(String node) {
            this.node = node;
        }

        public List<String> getProjects() {
            return projects;
        }

        public void setProjects(List<String> projects) {
            this.projects = projects;
        }
    }
}
