package cn.keepbx.jpom.model.data;

import cn.hutool.cron.pattern.CronPattern;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.model.BaseModel;

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
     * 通知方式
     * [{
     * style: 通知方式
     * value：电话或邮箱等
     * }]
     */
    private List<Notify> notify;
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

    public List<NodeProject> getProjects() {
        return projects;
    }

    public void setProjects(List<NodeProject> projects) {
        this.projects = projects;
    }

    public List<Notify> getNotify() {
        return notify;
    }

    public void setNotify(List<Notify> notify) {
        this.notify = notify;
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

    public enum Cycle implements BaseEnum {
        /**
         * 监控周期，code 代表周期时间，单位：分钟
         */
        one(1, "1分钟"),
        five(5, "5分钟"),
        ten(10, "10分钟"),
        thirty(30, "30分钟");

        private int code;
        private String desc;
        private CronPattern cronPattern;

        Cycle(int code, String desc) {
            this.code = code;
            this.desc = desc;
            this.cronPattern = new CronPattern(String.format("0 0/%s * * * ?", code));
        }

        public CronPattern getCronPattern() {
            return cronPattern;
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

    public enum NotifyType implements BaseEnum {
        /**
         * 通知方式
         */
        dingding(0, "钉钉"),
        mail(1, "邮箱"),
//        sms(2, "短信"),
        ;

        private int code;
        private String desc;

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

        public Notify(int style, String value) {
            this.style = style;
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
