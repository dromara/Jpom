package cn.keepbx.jpom.service.user;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.init.CheckRunCommand;
import cn.keepbx.jpom.util.JsonUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 用户管理
 *
 * @author Administrator
 */
@Service
public class UserService extends BaseDataService {

    private static final String FILENAME = ConfigBean.USER;

    /**
     * 用户列表是否为空
     *
     * @return true 为空需要初始化
     */
    public boolean userListEmpty() {
        try {
            JSONObject userInfo = getJsonObject(FILENAME);
            return userInfo.isEmpty();
        } catch (Exception ignored) {
        }
        return true;
    }

    /**
     * 用户登录
     *
     * @param name 用户名
     * @param pwd  密码
     * @return 登录
     */
    public UserModel login(String name, String pwd) throws Exception {
        JSONObject userInfo = getJsonObjectByKey(FILENAME, name);
        if (JsonUtil.jsonIsEmpty(userInfo)) {
            return null;
        }
        UserModel userModel = userInfo.toJavaObject(UserModel.class);
        if (pwd.equals(userModel.getPassword())) {
            return userModel;
        }
        return null;
    }

    /**
     * 验证用户md5
     *
     * @param userMd5 用户md5
     * @return userModel 用户对象
     * @throws IOException 异常
     */
    public UserModel checkUser(String userMd5) throws IOException {
        JSONObject jsonData = getJsonObject(FILENAME);
        if (jsonData == null) {
            return null;
        }
        for (String strKey : jsonData.keySet()) {
            JSONObject jsonUser = jsonData.getJSONObject(strKey);
            UserModel userModel = jsonUser.toJavaObject(UserModel.class);
            String strUserMd5 = userModel.getUserMd5Key();
            if (strUserMd5.equals(userMd5)) {
                return userModel;
            }
        }
        return null;
    }

    /**
     * 获取用户列表
     *
     * @return 用户列表
     */
    public JSONArray getUserList() {
        JSONObject jsonObject = null;
        try {
            jsonObject = getJsonObject(FILENAME);
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        if (jsonObject == null) {
            return null;
        }
        Set<Map.Entry<String, Object>> set = jsonObject.entrySet();
        JSONArray array = new JSONArray();
        for (Map.Entry entry : set) {
            JSONObject value = (JSONObject) entry.getValue();
            value.remove("password");
            array.add(value);
        }
        return array;
    }

    /**
     * 修改密码
     *
     * @param name   用户名
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return 修改结果
     */
    public String updatePwd(String name, String oldPwd, String newPwd) throws Exception {
        JSONObject userInfo = getJsonObjectByKey(FILENAME, name);
        // 判断用户是否存在
        if (null == userInfo) {
            return "notexist";
        }
        UserModel userModel = userInfo.toJavaObject(UserModel.class);
        if (oldPwd.equals(userModel.getPassword())) {
            // 修改密码
            userModel.setPassword(newPwd);
//            userInfo.put("password", newPwd);
            updateJson(FILENAME, userModel.toJson());
            return "success";
        } else {
            return "olderror";
        }
    }

    /**
     * 判断用户是否该项目的管理员
     *
     * @param projectId 项目id
     * @param userId    用户id
     * @return boolean
     */
    public boolean isManagerProject(String projectId, String userId) {
        UserModel user = getUserModel(userId);
        if (user == null) {
            return false;
        }
        boolean manage = user.isManage();
        if (manage) {
            return true;
        }
        JSONArray projects = user.getProjects();
        if (projects == null || projects.isEmpty()) {
            return false;
        }
        return projects.contains(projectId);
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户id
     * @return 用户信息
     */
    public UserModel getUserModel(String userId) {
        try {
            JSONObject jsonObject = getJsonObject(FILENAME);
            JSONObject user = jsonObject.getJSONObject(userId);
            if (user == null) {
                return null;
            }
            return user.toJavaObject(UserModel.class);
//            user.remove("password");
        } catch (IOException e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 删除用户
     *
     * @param id 用户id
     * @return String
     */
    public boolean deleteUser(String id) {
        try {
            deleteJson(FILENAME, id);
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 添加用户
     *
     * @param userModel 实体
     * @return true
     */
    public boolean addUser(UserModel userModel) {
        try {
            saveJson(FILENAME, userModel.toJson());
            return true;
        } catch (FileNotFoundException fileNotFoundException) {
            CheckRunCommand.repairData();
            return addUser(userModel);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 修改用户
     *
     * @return String
     */
    public boolean updateUser(UserModel userModel) {
        try {
            updateJson(FILENAME, userModel.toJson());
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }
}
