package cn.keepbx.jpom.model.data;

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
     *
     */
    private String userName;
    /**
     *
     */
    private String password;

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
}
