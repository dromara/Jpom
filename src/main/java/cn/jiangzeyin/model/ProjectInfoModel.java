package cn.jiangzeyin.model;

import com.alibaba.fastjson.JSONObject;

/**
 * 项目配置信息实体
 *
 * @author jiangzeyin
 */
public class ProjectInfoModel {
    private String id;
    private String name;
    private String group;
    private String mainClass;
    private String lib;
    private String log;
    private String jvm;
    private String token;
    private boolean status;
    private String createTime;
    private String modifyTime;
    private String args;
    private String buildTag;
    /**
     * lib 目录当前文件状态
     */
    private String useLibDesc;
    /**
     * 当前运行lib 状态
     */
    private String runLibDesc;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getRunLibDesc() {
        return runLibDesc;
    }

    public void setRunLibDesc(String runLibDesc) {
        this.runLibDesc = runLibDesc;
    }

    public String getUseLibDesc() {
        return useLibDesc;
    }

    public void setUseLibDesc(String useLibDesc) {
        this.useLibDesc = useLibDesc;
    }

    public String getBuildTag() {
        return buildTag;
    }

    public void setBuildTag(String buildTag) {
        this.buildTag = buildTag;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getJvm() {
        return jvm;
    }

    public void setJvm(String jvm) {
        this.jvm = jvm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getLib() {
        return lib;
    }

    public void setLib(String lib) {
        this.lib = lib;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    public JSONObject toJson() {
        return (JSONObject) JSONObject.toJSON(this);
    }
}
