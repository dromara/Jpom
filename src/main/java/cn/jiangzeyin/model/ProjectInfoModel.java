package cn.jiangzeyin.model;

public class ProjectInfoModel {
    private String id;
    private String tag;
    private String mainClass;
    private String lib;
    private String log;
    private String jvm;
    private int port;
    private String token;
    private String createTime;

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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    @Override
    public String toString() {
        return "ProjectInfoModel{" +
                "id='" + id + '\'' +
                ", tag='" + tag + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", lib='" + lib + '\'' +
                ", log='" + log + '\'' +
                ", jvm='" + jvm + '\'' +
                ", port=" + port +
                ", token='" + token + '\'' +
                ", createTime='" + createTime + '\'' +
                '}';
    }
}
