package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseJsonModel;

/**
 * 用户操作日志
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class UserOperateLogV1 extends BaseJsonModel {
    public static final String TABLE_NAME = UserOperateLogV1.class.getSimpleName().toUpperCase();

    private String ip;

    private String userId;

    private long optTime;
    private int optType = Type.Def.getCode();

    private int optStatus = Status.Fail.getCode();

    private String resultMsg;


    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getOptTime() {
        return optTime;
    }

    public void setOptTime(long optTime) {
        this.optTime = optTime;
    }

    public int getOptType() {
        return optType;
    }

    public void setOptType(int optType) {
        this.optType = optType;
    }

    public int getOptStatus() {
        return optStatus;
    }

    public void setOptStatus(int optStatus) {
        this.optStatus = optStatus;
    }

    public String getResultMsg() {
        return resultMsg;
    }

    public void setResultMsg(String resultMsg) {
        this.resultMsg = resultMsg;
    }

    public enum Type {
        /**
         *
         */
        Def(0, "未知类型"),
        Login(1, "账号登录"),
        EditNode(2, "修改节点");
        private int code;
        private String desc;

        public int getCode() {
            return code;
        }


        public String getDesc() {
            return desc;
        }


        Type(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    public enum Status {
        /**
         * 请求状态码200 为成功
         */
        Success(200, "成功"),
        Fail(0, "失败");
        private int code;
        private String desc;

        public int getCode() {
            return code;
        }


        public String getDesc() {
            return desc;
        }


        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
