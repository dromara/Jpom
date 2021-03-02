package io.jpom.model.data;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.SecureUtil;
import io.jpom.model.BaseModel;
import io.jpom.system.ServerExtConfigBean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户实体
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
public class UserModel extends BaseModel {
    /**
     * 系统管理员
     */
    public static String SYSTEM_ADMIN = "sys";
    /**
     * 系统占用名
     */
    public static String SYSTEM_OCCUPY_NAME = "系统管理员";
    /**
     * 用户名限制
     */
    public static int USER_NAME_MIN_LEN = 3;
    /**
     * 密码
     */
    private String password;
    /**
     * 创建此用户的人
     */
    private String parent;
    /**
     * 连续登录失败次数
     */
    private int pwdErrorCount;
    /**
     * 最后失败时间
     */
    private long lastPwdErrorTime;
    /**
     * 账号被锁定的时长
     */
    private long lockTime;
    /**
     * 记录最后修改时间
     */
    private long modifyTime;
    /**
     * 角色
     */
    private Set<String> roles;
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

    public long getLockTime() {
        return lockTime;
    }

    public long getLastPwdErrorTime() {
        return lastPwdErrorTime;
    }

    public void setLastPwdErrorTime(long lastPwdErrorTime) {
        this.lastPwdErrorTime = lastPwdErrorTime;
    }

    public void setLockTime(long lockTime) {
        this.lockTime = lockTime;
    }

    public int getPwdErrorCount() {
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
        setLastPwdErrorTime(0);
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
        long lastTime = getLastPwdErrorTime();
        if (lastTime <= 0) {
            return 0;
        }
        // 当前锁定时间
        long lockTime = getLockTime();
        if (lockTime <= 0) {
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
        this.setModifyTime(DateUtil.current());
    }

    public String getUserMd5Key() {
        return SecureUtil.md5(String.format("%s:%s", getId(), password));
    }

    public boolean isSystemUser() {
        return UserModel.SYSTEM_ADMIN.equals(getParent());
    }

    /**
     * demo 登录名默认为系统验证账号
     *
     * @return true
     */
    public boolean isDemoUser() {
        return "demo".equals(getId());
    }

    public long getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(long modifyTime) {
        this.modifyTime = modifyTime;
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = new HashSet<>(roles);
    }
}
