//package io.jpom.model.data;
//
//import io.jpom.build.BaseBuildModule;
//
///**
// * 在线构建
// *
// * @author bwcx_jzy
// * @date 2019/7/10
// **/
//@Deprecated
//public class BuildModel extends BaseBuildModule {
//
//	/**
//	 * 仓库类型
//	 */
//	private int repoType;
//	/**
//	 * 仓库地址 git 或者 svn
//	 */
//	private String gitUrl;
//	/**
//	 * 拉取的分支名称
//	 */
//	private String branchName;
//	/**
//	 * 账号
//	 */
//	private String userName;
//	/**
//	 * 密码
//	 */
//	private String password;
//	/**
//	 * 修改人
//	 */
//	private String modifyUser;
//	/**
//	 * 修改时间
//	 */
//	private String modifyTime;
//	/**
//	 * 构建状态
//	 */
//	private int status;
//	/**
//	 * 构建命令
//	 */
//	private String script;
//	/**
//	 * 构建id
//	 */
//	private int buildId;
//	/**
//	 * 触发器token
//	 */
//	private String triggerToken;
//
//	/**
//	 * 分组
//	 */
//	private String group;
//
//	public String getTriggerToken() {
//		return triggerToken;
//	}
//
//	public void setTriggerToken(String triggerToken) {
//		this.triggerToken = triggerToken;
//	}
//
//	public int getBuildId() {
//		return buildId;
//	}
//
//	public String getBuildIdStr() {
//		return getBuildIdStr(buildId);
//	}
//
//	public static String getBuildIdStr(int buildId) {
//		return String.format("#%s", buildId);
//	}
//
//	public void setBuildId(int buildId) {
//		this.buildId = buildId;
//	}
//
//	public String getScript() {
//		return script;
//	}
//
//	public void setScript(String script) {
//		this.script = script;
//	}
//
//	public int getStatus() {
//		return status;
//	}
//
//	public void setStatus(int status) {
//		this.status = status;
//	}
//
//	public String getModifyUser() {
//		return modifyUser;
//	}
//
//	public void setModifyUser(String modifyUser) {
//		this.modifyUser = modifyUser;
//	}
//
//	public String getModifyTime() {
//		return modifyTime;
//	}
//
//	public void setModifyTime(String modifyTime) {
//		this.modifyTime = modifyTime;
//	}
//
//	public String getGitUrl() {
//		return gitUrl;
//	}
//
//	public void setGitUrl(String gitUrl) {
//		this.gitUrl = gitUrl;
//	}
//
//	public String getBranchName() {
//		return branchName;
//	}
//
//	public void setBranchName(String branchName) {
//		this.branchName = branchName;
//	}
//
//	public String getUserName() {
//		return userName;
//	}
//
//	public void setUserName(String userName) {
//		this.userName = userName;
//	}
//
//	public String getPassword() {
//		return password;
//	}
//
//	public void setPassword(String password) {
//		this.password = password;
//	}
//
//	public int getRepoType() {
//		return repoType;
//	}
//
//	public void setRepoType(int repoType) {
//		this.repoType = repoType;
//	}
//
//	public String getGroup() {
//		return group;
//	}
//
//	public void setGroup(String group) {
//		this.group = group;
//	}
//}
