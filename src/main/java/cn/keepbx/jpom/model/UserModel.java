package cn.keepbx.jpom.model;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 用户实体
 *
 * @author jiangzeyin
 * @date 2019/1/16
 */
public class UserModel {
    private String id;
    private String name;
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
     * 系统管理员
     */
    public static String SYSTEM_ADMIN = "sys";
    /**
     * 用户密码长度
     */
    public static int USER_PWD_LEN = 6;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
        return SecureUtil.md5(String.format("%s:%s", id, password));
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
