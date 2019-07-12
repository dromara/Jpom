package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.BaseModel;
import com.alibaba.fastjson.JSONArray;

/**
 * 监控管理实体
 *
 * @author Arno
 */
public class MonitorModel extends BaseModel {
    /**
     * 监控的项目
     */
    private JSONArray projects;
    /**
     * 通知方式
     * [{
     * style: 通知方式
     * value：电话或邮箱等
     * }]
     */
    private JSONArray notify;
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

    public JSONArray getProjects() {
        return projects;
    }

    public void setProjects(JSONArray projects) {
        this.projects = projects;
    }

    public JSONArray getNotify() {
        return notify;
    }

    public void setNotify(JSONArray notify) {
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

        Cycle(int code, String desc) {
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

    public enum NotifyType implements BaseEnum {
        /**
         * 通知方式
         */
        dingding(0, "钉钉"),
        mail(1, "邮箱"),
        sms(2, "短信");

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

}
