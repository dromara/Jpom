package io.jpom.model.log;

import cn.hutool.core.util.StrUtil;
import io.jpom.JpomApplication;
import io.jpom.model.BaseDbModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.h2db.TableName;

/**
 * ssh 终端执行日志
 *
 * @author jiangzeyin
 * @date 2021/08/04
 */
@TableName("SSHTERMINALEXECUTELOG")
public class SshTerminalExecuteLog extends BaseDbModel {
	/**
	 * 操作ip
	 */
	private String ip;
	/**
	 * 用户ip
	 */
	private String userId;
	/**
	 * sshid
	 */
	private String sshId;
	/**
	 * 名称
	 */
	private String sshName;
	/**
	 * 操作时间
	 */
	private long optTime;
	/**
	 * 执行的命令
	 */
	private String commands;
	/**
	 * 浏览器标识
	 */
	private String userAgent;

	/**
	 * 是否拒绝执行
	 */
	private Boolean refuse;

	public Boolean getRefuse() {
		return refuse;
	}

	public void setRefuse(Boolean refuse) {
		this.refuse = refuse;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = StrUtil.maxLength(userAgent, 280);
	}


	/**
	 * 操作id
	 */
	public SshTerminalExecuteLog() {
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
		if (UserModel.SYSTEM_OCCUPY_NAME.equals(userId)) {
			this.userId = JpomApplication.SYSTEM_ID;
		} else {
			this.userId = userId;
		}
	}

	public long getOptTime() {
		return optTime;
	}

	public void setOptTime(long optTime) {
		this.optTime = optTime;
	}

	public String getSshId() {
		return sshId;
	}

	public void setSshId(String sshId) {
		this.sshId = sshId;
	}

	public String getCommands() {
		return commands;
	}

	public void setCommands(String commands) {
		this.commands = commands;
	}


	public String getSshName() {
		return sshName;
	}

	public void setSshName(String sshName) {
		this.sshName = sshName;
	}
}
