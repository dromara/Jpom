package io.jpom.model.data;

import io.jpom.model.BaseModel;

/**
 * @author Hotstrip
 * 仓库地址实体类
 */
public class RepositoryModel extends BaseModel {
	public static final String TABLE_NAME = "REPOSITORY";
	/**
	 * 仓库地址
	 */
	private String gitUrl;
	/**
	 * 仓库类型{0: GIT, 1: SVN}
	 */
	private Integer repoType;
	/**
	 * 拉取代码的协议{0: http, 1: ssh}
	 */
	private Integer protocol;
	/**
	 * 登录用户
	 */
	private String userName;
	/**
	 * 登录密码
	 */
	private String password;
	/**
	 * SSH RSA 公钥
	 */
	private String rsaPub;
	/**
	 * @return
	 */
	private String modifyTime;

	public String getGitUrl() {
		return gitUrl;
	}

	public void setGitUrl(String gitUrl) {
		this.gitUrl = gitUrl;
	}

	public Integer getRepoType() {
		return repoType;
	}

	public void setRepoType(Integer repoType) {
		this.repoType = repoType;
	}

	public Integer getProtocol() {
		return protocol;
	}

	public void setProtocol(Integer protocol) {
		this.protocol = protocol;
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

	public String getRsaPub() {
		return rsaPub;
	}

	public void setRsaPub(String rsaPub) {
		this.rsaPub = rsaPub;
	}

	public String getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(String modifyTime) {
		this.modifyTime = modifyTime;
	}
}
