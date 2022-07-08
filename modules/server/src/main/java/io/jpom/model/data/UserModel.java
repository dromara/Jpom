/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
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
import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseStrikeDbModel;
import io.jpom.service.h2db.TableName;
import io.jpom.system.ServerExtConfigBean;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.concurrent.TimeUnit;

/**
 * 用户实体
 *
 * @author jiangzeyin
 * @since 2019/1/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "USER_INFO", name = "用户账号")
@Data
public class UserModel extends BaseStrikeDbModel {
    /**
     * 系统管理员
     */
    public final static String SYSTEM_ADMIN = "sys";
    /**
     * demo 演示账号、系统预设
     */
    public final static String DEMO_USER = "demo";
    /**
     *
     */
    public static final UserModel EMPTY = new UserModel(UserModel.SYSTEM_ADMIN);
    /**
     * 系统占用名
     */
    public static final String SYSTEM_OCCUPY_NAME = "系统管理员";
    /**
     * 用户名限制
     */
    public static final int USER_NAME_MIN_LEN = 3;
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
    /**
     * 密码盐值
     */
    private String salt;
    /**
     * 两步验证 key
     */
    private String twoFactorAuthKey;
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

    public void setSystemUser(Integer systemUser) {
        this.systemUser = ObjectUtil.defaultIfNull(systemUser, 0) == 1 ? systemUser : 0;
    }

//	/**
//	 * 解锁
//	 */
//	public UserModel unLock() {
//		UserModel newModel = new UserModel(this.getId());
//		return UserModel.unLock(newModel);
//	}

    /**
     * 解锁
     */
    public static UserModel unLock(String id) {
        UserModel newModel = new UserModel(id);
        newModel.setPwdErrorCount(0);
        newModel.setLockTime(0L);
        newModel.setLastPwdErrorTime(0L);
        return newModel;
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
    public UserModel errorLock() {
        // 未开启锁定功能
        int userAlwaysLoginError = ServerExtConfigBean.getInstance().userAlwaysLoginError;
        if (userAlwaysLoginError <= 0) {
            return null;
        }
        UserModel newModel = new UserModel(this.getId());
        newModel.setPwdErrorCount(ObjectUtil.defaultIfNull(this.getPwdErrorCount(), 0) + 1);
        int count = newModel.getPwdErrorCount();
        // 记录错误时间
        newModel.setLastPwdErrorTime(DateUtil.currentSeconds());
        if (count < userAlwaysLoginError) {
            // 还未达到锁定条件
            return newModel;
        }
        int level = count / userAlwaysLoginError;
        switch (level) {
            case 1:
                // 在错误倍数 为1 锁定 30分钟
                newModel.setLockTime(TimeUnit.MINUTES.toSeconds(30));
                break;
            case 2:
                // 在错误倍数 为2 锁定 1小时
                newModel.setLockTime(TimeUnit.HOURS.toSeconds(1));
                break;
            default:
                // 其他情况 10小时
                newModel.setLockTime(TimeUnit.HOURS.toSeconds(10));
                break;
        }
        return newModel;
    }

    public boolean isSystemUser() {
        return systemUser != null && systemUser == 1;
    }

    /**
     * 是否为超级管理员
     *
     * @return true 是
     */
    public boolean isSuperSystemUser() {
        return StrUtil.equals(getParent(), SYSTEM_ADMIN);
    }

    /**
     * demo 登录名默认为系统演示账号
     *
     * @return true
     */
    public boolean isDemoUser() {
        // demo 账号 和他创建的账号都是 demo
        return isRealDemoUser() || UserModel.DEMO_USER.equals(getParent());
    }

    /**
     * demo 登录名默认为系统演示账号
     *
     * @return true
     */
    public boolean isRealDemoUser() {
        return UserModel.DEMO_USER.equals(getId());
    }

//	/**
//	 * 隐藏系统管理的真实id
//	 *
//	 * @param userModel 实体
//	 * @return 系统管理员返回默认
//	 */
//	public static String getOptUserName(UserModel userModel) {
//		String userId;
//		if (userModel.isSystemUser()) {
//			userId = UserModel.SYSTEM_OCCUPY_NAME;
//		} else {
//			userId = userModel.getId();
//		}
//		return userId;
//	}
}
