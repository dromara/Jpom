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
     * 用于socket 回话回调更新
     */
    private String reqId;
    /**
     * 请求参数
     */
    private String reqData;
    /**
     * 数据id
     */
    private String dataId;
    /**
     * 浏览器标识
     */
    private String userAgent;

    public String getReqData() {
        return reqData;
    }

    public void setReqData(String reqData) {
        this.reqData = StrUtil.maxLength(reqData, 999999990);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = StrUtil.maxLength(userAgent, 280);
    }

    public String getDataId() {
        return StrUtil.emptyToDefault(dataId, StrUtil.DASHED);
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public UserOperateLogV1(String reqId) {
        if (reqId == null) {
            this.reqId = IdUtil.fastUUID();
        } else {
            this.reqId = reqId;
        }
    }

    /**
     * 操作id
     */
    public UserOperateLogV1() {
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

    /**
     * 获取执行结果的描述消息
     *
     * @return 成功/ 失败：状态码
     */
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
        this.resultMsg = StrUtil.maxLength(resultMsg, 999999990);
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

        SaveOutGiving(18, "保存节点分发"),
        DelOutGiving(19, "删除节点分发"),


        Start(20, "启动项目"),
        Stop(21, "停止项目"),
        Restart(22, "重启项目"),

        SaveProject(23, "修改项目"),

        UploadOutGiving(24, "分发文件"),

        UploadProjectFile(25, "上传项目文件"),
        DownloadProjectFile(26, "下载项目文件"),
        DelProjectFile(27, "删除项目文件"),
        ExportProjectLog(28, "导出项目日志"),
        DownloadProjectLogBack(29, "下载项目备份日志"),
        DelProjectLogBack(30, "删除项目备份日志"),
        RestProjectLog(31, "重置项目日志"),

        BuildDownload(32, "构建下载"),
        BuildInstall(33, "构建安装"),

        ExportStack(34, "导出项目栈"),
        ExportRam(35, "导出项目内容"),

        SaveOutgivingWhitelist(36, "修改节点白名单"),
        SaveOutgivingProject(37, "保存节点分发项目"),
        DeleteOutgivingProject(38, "删除节点分发项目"),

        Save_Script(39, "保存脚本模板"),

        Script_Start(40,"执行脚本"),
        Script_Stop(41,"停止脚本"),

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

    /**
     * 状态状态
     */
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
