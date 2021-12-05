/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.model.data;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import io.jpom.model.BaseStrikeDbModel;
import io.jpom.service.h2db.TableName;
import io.jpom.system.ServerExtConfigBean;

import java.util.concurrent.TimeUnit;

/**
 * 用户实体
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
@TableName("USER_INFO")
public class UserModel extends BaseStrikeDbModel {
	/**
	 * 系统管理员
	 */
	public static String SYSTEM_ADMIN = "sys";
	/**
	 *
	 */
	public static final UserModel EMPTY = new UserModel(UserModel.SYSTEM_ADMIN);
	/**
	 * 系统占用名
	 */
	public static String SYSTEM_OCCUPY_NAME = "系统管理员";
	/**
	 * 用户名限制
	 */
	public static int USER_NAME_MIN_LEN = 3;
	/**
	 * 盐值长度
	 */
	public static int SALT_LEN = 8;
	/**
	 * 昵称
	 */
	private String name;
	/**
	 * 密码
	 */
	private String password;
	private String salt;
	/**
	 * 创建此用户的人
	 */
	private String parent;
	/**
	 * 系统管理员
	 */
	private Integer systemUser;
	/**
	 * 连续登录失败次数
	 */
	private Integer pwdErrorCount;
	/**
	 * 最后失败时间
	 */
	private Long lastPwdErrorTime;
	/**
	 * 账号被锁定的时长
	 */
	private Long lockTime;
	/**
	 * 邮箱
	 */
	private String email;
	/**
	 * 钉钉
	 */
	private String dingDing;
	/**
	 * 企业微信
	 */
	private String workWx;

	public UserModel(String id) {
		this.setId(id);
	}

	public UserModel() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public Integer getSystemUser() {
		return systemUser;
	}

	public void setSystemUser(Integer systemUser) {
		this.systemUser = ObjectUtil.defaultIfNull(systemUser, 0) == 1 ? systemUser : 0;
	}

	public Long getLockTime() {
		return lockTime;
	}

	public Long getLastPwdErrorTime() {
		return lastPwdErrorTime;
	}

	public void setLastPwdErrorTime(Long lastPwdErrorTime) {
		this.lastPwdErrorTime = lastPwdErrorTime;
	}

	public void setLockTime(long lockTime) {
		this.lockTime = lockTime;
	}

	public Integer getPwdErrorCount() {
		return pwdErrorCount;
	}

	public void setPwdErrorCount(int pwdErrorCount) {
		this.pwdErrorCount = pwdErrorCount;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDingDing() {
		return dingDing;
	}

	public void setDingDing(String dingDing) {
		this.dingDing = dingDing;
	}

	public String getWorkWx() {
		return workWx;
	}

	public void setWorkWx(String workWx) {
		this.workWx = workWx;
	}

	/**
	 * 解锁
	 */
	public void unLock() {
		setPwdErrorCount(0);
		setLockTime(0);
		setLastPwdErrorTime(0L);
	}

	/**
	 * 剩余解锁时间
	 *
	 * @return 0是未锁定
	 */
	public long overLockTime() {
		if (ServerExtConfigBean.getInstance().userAlwaysLoginError <= 0) {
			return 0;
		}
		// 不限制演示账号的登录
		if (isDemoUser()) {
			return 0;
		}
		// 最后一次失败时间
		Long lastTime = getLastPwdErrorTime();
		if (lastTime == null || lastTime <= 0) {
			return 0;
		}
		// 当前锁定时间
		Long lockTime = getLockTime();
		if (lockTime == null || lockTime <= 0) {
			return 0;
		}
		// 解锁时间
		lastTime += lockTime;
		long nowTime = DateUtil.currentSeconds();
		// 剩余解锁时间
		lastTime -= nowTime;
		if (lastTime > 0) {
			return lastTime;
		}
		return 0;
	}

	/**
	 * 登录失败，重新计算锁定时间
	 */
	public void errorLock() {
		// 未开启锁定功能
		if (ServerExtConfigBean.getInstance().userAlwaysLoginError <= 0) {
			return;
		}
		setPwdErrorCount(getPwdErrorCount() + 1);
		int count = getPwdErrorCount();
		// 记录错误时间
		setLastPwdErrorTime(DateUtil.currentSeconds());
		if (count < ServerExtConfigBean.getInstance().userAlwaysLoginError) {
			// 还未达到锁定条件
			return;
		}
		int level = count / ServerExtConfigBean.getInstance().userAlwaysLoginError;
		switch (level) {
			case 1:
				// 在错误倍数 为1 锁定 30分钟
				setLockTime(TimeUnit.MINUTES.toSeconds(30));
				break;
			case 2:
				// 在错误倍数 为2 锁定 1小时
				setLockTime(TimeUnit.HOURS.toSeconds(1));
				break;
			default:
				// 其他情况 10小时
				setLockTime(TimeUnit.HOURS.toSeconds(10));
				break;
		}
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
		// 记录修改时间，如果在线用户线退出
		//this.setModifyTime(DateUtil.current());
	}

	public boolean isSystemUser() {
		return systemUser != null && systemUser == 1;
	}

	/**
	 * demo 登录名默认为系统验证账号
	 *
	 * @return true
	 */
	public boolean isDemoUser() {
		return "demo".equals(getId());
	}

	/**
	 * 隐藏系统管理的真实id
	 *
	 * @param userModel 实体
	 * @return 系统管理员返回默认
	 */
	public static String getOptUserName(UserModel userModel) {
		String userId;
		if (userModel.isSystemUser()) {
			userId = UserModel.SYSTEM_OCCUPY_NAME;
		} else {
			userId = userModel.getId();
		}
		return userId;
	}
}
