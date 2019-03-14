package cn.keepbx.jpom.model;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keepbx.jpom.system.ExtConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

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
     * 用户密码长度
     */
    public static int USER_PWD_LEN = 6;

    /**
     * 昵称
     */
    private String name;
    /**
     * 密码
     */
    private String password;
    /**
     * 是否为管理员
     */
    private boolean manage;
    /**
     * 授权的项目集
     */
    private JSONArray projects;
    /**
     * 创建此用户的人
     */
    private String parent;
    /**
     * 连续登录失败次数
     */
    private int pwdErrorCount;
    private long lastPwdErrorTime;
    private long lockTime;

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
        if (ExtConfigBean.getInstance().userAlwaysLoginError <= 0) {
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
        if (ExtConfigBean.getInstance().userAlwaysLoginError <= 0) {
            return;
        }
        setPwdErrorCount(getPwdErrorCount() + 1);
        int count = getPwdErrorCount();
        // 记录错误时间
        setLastPwdErrorTime(DateUtil.currentSeconds());
        if (count < ExtConfigBean.getInstance().userAlwaysLoginError) {
            // 还未达到锁定条件
            return;
        }
        int level = count / ExtConfigBean.getInstance().userAlwaysLoginError;
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

    public JSONArray getProjects() {
        return projects;
    }

    public void setProjects(JSONArray projects) {
        this.projects = projects;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserMd5Key() {
        return SecureUtil.md5(String.format("%s:%s", getId(), password));
    }

    public boolean isManage() {
        return manage;
    }

    public boolean isProject(String id) {
        if (isManage()) {
            return true;
        }
        if (projects == null) {
            return false;
        }
        return projects.contains(id);
    }

    public void setManage(boolean manage) {
        this.manage = manage;
    }

    public JSONObject toJson() {
        return (JSONObject) JSONObject.toJSON(this);
    }
}
