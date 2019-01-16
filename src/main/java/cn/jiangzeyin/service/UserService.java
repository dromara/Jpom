package cn.jiangzeyin.service;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.model.UserModel;
import cn.jiangzeyin.system.ConfigBean;
import cn.jiangzeyin.util.JsonUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author Administrator
 */
@Service
public class UserService extends BaseDataService {

    private static final String FILENAME = ConfigBean.USER;

    /**
     * 用户登录
     *
     * @param name 用户名
     * @param pwd  密码
     * @return 登录
     */
    public boolean login(String name, String pwd) throws Exception {
        JSONObject userInfo = getJsonObject(FILENAME, name);
        if (JsonUtil.jsonIsEmpty(userInfo)) {
            return false;
        }
        UserModel userModel = userInfo.toJavaObject(UserModel.class);
        return pwd.equals(userModel.getPassword());
    }


    public boolean checkUser(String userMd5) throws IOException {
        JSONObject jsonData = getJsonObject(FILENAME);
        if (jsonData == null) {
            return false;
        }
        for (String strKey : jsonData.keySet()) {
            JSONObject jsonUser = jsonData.getJSONObject(strKey);
            UserModel userModel = jsonUser.toJavaObject(UserModel.class);
            String strUsermd5 = userModel.getUserMd5Key();
            if (strUsermd5.equals(userMd5)) {
                return true;
            }
        }
        return false;
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
            e.printStackTrace();
        }
        if (jsonObject == null) {
            return null;
        }
        Set<Map.Entry<String, Object>> set = jsonObject.entrySet();
        JSONArray array = new JSONArray();
        for (Map.Entry entry : set) {
            String key = (String) entry.getKey();
            JSONObject value = (JSONObject) entry.getValue();
            value.remove("password");
            if (value.containsKey("role")) {
                value.put("role", "是");
            } else {
                value.put("role", "否");
            }
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
     * @return
     */
    public String updatePwd(String name, String oldPwd, String newPwd) throws Exception {
        JSONObject userInfo = getJsonObject(FILENAME, name);

        // 判断用户是否存在
        if (null == userInfo) {
            return "notexist";
        }
        if (oldPwd.equals(userInfo.getString("password"))) {
            // 修改密码
            userInfo.put("password", newPwd);
            updateJson(FILENAME, userInfo);
            return "success";
        } else {
            return "olderror";
        }
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
     * 新增用户
     */
    public boolean addUser(String id, String name, String password, String role) {
        try {
            UserModel userModel = new UserModel();
            userModel.setName(name);
            userModel.setId(id);
            userModel.setPassword(password);
            saveJson(FILENAME, userModel.toJson());
            return true;
        } catch (Exception e) {
            DefaultSystemLog.LOG().error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 修改用户
     *
     * @return String
     */
    public boolean updateUser(String id, String name, String password, String role) {
        try {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("name", name);
            object.put("role", role);
            JSONObject jsonObject = getJsonObject(FILENAME);
            JSONObject user = jsonObject.getJSONObject(id);
            String pass = user.getString("password");
            object.put("password", pass);
            if (!StrUtil.isEmpty(password)) {
                object.put("password", password);
            }
            updateJson(FILENAME, object);
            return true;
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
        }
        return false;
    }
}
