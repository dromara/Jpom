package cn.keepbx.jpom.model.data;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.crypto.SecureUtil;
import cn.keepbx.jpom.model.BaseJsonModel;
import cn.keepbx.jpom.model.BaseModel;
import cn.keepbx.jpom.socket.CommonSocketConfig;
import cn.keepbx.jpom.system.ServerExtConfigBean;
import com.alibaba.fastjson.JSONArray;

import java.util.Map;
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
     * 节点权限
     */
    private Map<String, NodeRole> nodeRole;
    /**
     * 服务管理员
     */
    private boolean serverManager;

    public boolean isServerManager() {
        if (isSystemUser()) {
            return true;
        }
        return serverManager;
    }

    public void setServerManager(boolean serverManager) {
        this.serverManager = serverManager;
    }

    public Map<String, NodeRole> getNodeRole() {
        if (nodeRole == null) {
            nodeRole = MapUtil.newHashMap();
        }
        return nodeRole;
    }

    public void putNodeRole(NodeRole addNodeRole) {
        if (nodeRole == null) {
            nodeRole = MapUtil.newHashMap();
        }
        nodeRole.put(addNodeRole.id, addNodeRole);
    }

    public void removeNodeRole(String id) {
        if (nodeRole != null) {
            nodeRole.remove(id);
        }
    }

    public void setNodeRole(Map<String, NodeRole> nodeRole) {
        this.nodeRole = nodeRole;
    }

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
        this.setModifyTime(DateUtil.current(false));
    }

    public String getUserMd5Key() {
        return SecureUtil.md5(String.format("%s:%s", getId(), password));
    }

    /**
     * 是否为节点管理员
     *
     * @return true 是
     */
    public boolean isManage(String nodeId) {
        if (isSystemUser()) {
            return true;
        }
        if (nodeRole == null) {
            return false;
        }
        NodeRole item = nodeRole.get(nodeId);
        if (item == null) {
            return false;
        }
        return item.manage;
    }

    /**
     * 是否能管理某个项目
     *
     * @param id 项目Id
     * @return true 能管理，管理员所有项目都能管理
     * @
     */
    public boolean isProject(String nodeId, String id) {
        if (isManage(nodeId)) {
            return true;
        }
        // 系统监控权限
        if (CommonSocketConfig.SYSTEM_ID.equals(id)) {
            return true;
        }
        NodeRole item = nodeRole.get(nodeId);
        if (item == null) {
            return false;
        }
        if (item.projects == null) {
            return false;
        }
        return item.projects.contains(id);
    }

    /**
     * 获取是否有上传文件的权限
     *
     * @return 系统管理员都用权限
     */
    public boolean isUploadFile(String nodeId) {
        if (isSystemUser()) {
            return true;
        }
        NodeRole item = nodeRole.get(nodeId);
        if (item == null) {
            return false;
        }
        return item.uploadFile;
    }

    /**
     * 获取是否有删除文件的权限
     *
     * @return 系统管理员都用权限
     */
    public boolean isDeleteFile(String nodeId) {
        if (isSystemUser()) {
            return true;
        }
        NodeRole item = nodeRole.get(nodeId);
        if (item == null) {
            return false;
        }
        return item.deleteFile;
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

    /**
     * 节点权限
     */
    public static class NodeRole extends BaseJsonModel {
        private String id;

        /**
         * 是否为管理员
         */
        private boolean manage;
        /**
         * 上传文件权限
         */
        private boolean uploadFile;
        /**
         * 删除文件权限
         */
        private boolean deleteFile;

        public void setManage(boolean manage) {
            this.manage = manage;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        /**
         * 获取是否有上传文件的权限
         *
         * @return 系统管理员都用权限
         */
        public boolean isUploadFile() {
            return uploadFile;
        }

        public void setUploadFile(boolean uploadFile) {
            this.uploadFile = uploadFile;
        }

        /**
         * 获取是否有删除文件的权限
         *
         * @return 系统管理员都用权限
         */
        public boolean isDeleteFile() {
            return deleteFile;
        }

        public void setDeleteFile(boolean deleteFile) {
            this.deleteFile = deleteFile;
        }

        /**
         * 是否为管理员
         *
         * @return true 是
         */
        public boolean isManage() {
            return manage;
        }

        /**
         * 授权的项目集
         */
        private JSONArray projects;


        public JSONArray getProjects() {
            return projects;
        }

        public void setProjects(JSONArray projects) {
            this.projects = projects;
        }
    }
}
