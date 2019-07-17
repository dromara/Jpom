package cn.keepbx.jpom.model.data;

import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.BaseModel;

/**
 * 在线构建
 *
 * @author bwcx_jzy
 * @date 2019/7/10
 **/
public class BuildModel extends BaseModel {

    /**
     * 仓库地址
     */
    private String gitUrl;
    /**
     * 拉取的分支名称
     */
    private String branchName;
    /**
     * 结果目录 或者文件
     */
    private String resultDirFile;
    /**
     * 账号
     */
    private String userName;
    /**
     * 密码
     */
    private String password;
    /**
     * 修改人
     */
    private String modifyUser;
    /**
     * 修改时间
     */
    private String modifyTime;
    /**
     * 构建状态
     */
    private int status = Status.No.getCode();
    /**
     * 构建命令
     */
    private String script;
    /**
     * 构建id
     */
    private int buildId;

    public int getBuildId() {
        return buildId;
    }

    public String getBuildIdStr() {
        return getBuildIdStr(buildId);
    }

    public static String getBuildIdStr(int buildId) {
        return String.format("#%s", buildId);
    }

    public void setBuildId(int buildId) {
        this.buildId = buildId;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getModifyUser() {
        return modifyUser;
    }

    public void setModifyUser(String modifyUser) {
        this.modifyUser = modifyUser;
    }

    public String getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(String modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getResultDirFile() {
        return resultDirFile;
    }

    public void setResultDirFile(String resultDirFile) {
        this.resultDirFile = resultDirFile;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public enum Status implements BaseEnum {
        /**
         *
         */
        No(0, "未构建"),
        Ing(1, "构建中"),
        Success(2, "构建成功"),
        Error(3, "构建失败"),
        PubIng(4, "发布中"),
        PubSuccess(5, "发布成功"),
        PubError(6, "发布失败"),
        ;
        private int code;
        private String desc;

        Status(int code, String desc) {
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
