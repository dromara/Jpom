package io.jpom.model.data;

import cn.hutool.http.HttpUtil;
import io.jpom.model.BaseDbModel;
import io.jpom.model.BaseEnum;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.service.h2db.TableName;

/**
 * @author Hotstrip
 * 仓库地址实体类
 */
@TableName("REPOSITORY")
public class RepositoryModel extends BaseDbModel {
	/**
	 * 名称
	 */
	private String name;
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
	 *
	 * @see GitProtocolEnum
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
	 * SSH RSA 私钥
	 */
	private String rsaPrv;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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

	/**
	 * 返回协议类型，如果为 null 会尝试识别 http
	 *
	 * @return 枚举的值（1/0）
	 * @see GitProtocolEnum
	 */
	public Integer getProtocol() {
		if (protocol != null) {
			return protocol;
		}
		String gitUrl = this.getGitUrl();
		if (HttpUtil.isHttps(gitUrl) || HttpUtil.isHttp(gitUrl)) {
			return GitProtocolEnum.HTTP.getCode();
		}
		return null;
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

	public String getRsaPrv() {
		return rsaPrv;
	}

	public void setRsaPrv(String rsaPrv) {
		this.rsaPrv = rsaPrv;
	}

	/**
	 * 仓库类型
	 */
	public enum RepoType implements BaseEnum {
		/**
		 * git
		 */
		Git(0, "Git"),
		Svn(1, "Svn"),
		;
		private int code;
		private String desc;

		RepoType(int code, String desc) {
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
