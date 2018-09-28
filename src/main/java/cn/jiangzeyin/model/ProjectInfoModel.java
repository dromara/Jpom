package cn.jiangzeyin.model;

import com.alibaba.fastjson.JSONObject;

/**
 * 项目配置信息实体
 *
 * @author jiangzeyin
 */
public class ProjectInfoModel {
    private String id;
    private String tag;
    private String mainClass;
    private String lib;
    private String log;
    private String jvm;
    private String token;
    private String createTime;
    private String modifyTime;
    private String args;

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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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
}
