package cn.keepbx.jpom.model.data;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.BaseJsonModel;

/**
 * 用户操作日志
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class UserOperateLogV1 extends BaseJsonModel {
    public static final String TABLE_NAME = UserOperateLogV1.class.getSimpleName().toUpperCase();
    /**
     * 操作ip
     */
    private String ip;
    /**
     * 用户ip
     */
    private String userId;
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 操作时间
     */
    private long optTime;
    /**
     * 操作类型
     */
    private int optType = OptType.Def.getCode();
    /**
     * 操作状态
     */
    private int optStatus = Status.Fail.getCode();
    /**
     * 完整消息
     */
    private String resultMsg;
    /**
     * 操作id
     */
    private String reqId;

    public UserOperateLogV1(String reqId) {
        this.reqId = reqId;
    }

    public UserOperateLogV1() {
        this(IdUtil.fastUUID());
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getNodeId() {
        return StrUtil.emptyToDefault(nodeId, StrUtil.DASHED);
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

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

    public String getOptTypeMsg() {
        return BaseEnum.getDescByCode(OptType.class, getOptType());
    }

    public void setOptType(int optType) {
        this.optType = optType;
    }

    public int getOptStatus() {
        return optStatus;
    }

    public String getOptStatusMsg() {
        if (getOptStatus() == Status.Success.getCode()) {
            return Status.Success.getDesc();
        }
        return Status.Fail.getDesc() + ":" + getOptStatus();
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

    /**
     * 操作的类型
     */
    public enum OptType implements BaseEnum {
        /**
         *
         */
        Def(0, "未知类型"),
        Login(1, "账号登录"),
        EditNode(2, "修改节点"),
        DelNode(3, "删除节点"),
        DelProject(4, "删除项目"),
        //        SelectUser(5, "查询用户"),
        DelUer(6, "删除用户"),
        AddUer(7, "新增用户"),
        EditUer(8, "修改用户"),
        UnlockUer(9, "解锁用户"),
        InstallNode(10, "初始化节点"),
        EditWhitelist(11, "修改白名单"),
        EditAliOss(12, "修改阿里云OSS"),
        SaveCert(13, "修改证书"),
        DelCert(14, "删除证书"),
        ExportCert(15, "导出证书"),
        SaveNginx(16, "修改Nginx"),
        DelNginx(17, "删除Nginx"),

        Start(20, "启动项目"),
        Stop(21, "停止项目"),
        Restart(22, "重启项目"),
        ;
        private int code;
        private String desc;

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }


        OptType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    public enum Status implements BaseEnum {
        /**
         * 请求状态码200 为成功
         */
        Success(200, "成功"),
        Fail(0, "失败");
        private int code;
        private String desc;

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getDesc() {
            return desc;
        }


        Status(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }
}
