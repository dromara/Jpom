package cn.jiangzeyin.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.model.UserInfoMode;
import cn.jiangzeyin.util.JsonUtil;
import com.alibaba.fastjson.JSON;
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
public class UserService extends BaseService {

    private static final String FILENAME = "user.json";

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
        return pwd.equals(userInfo.getString("password"));
    }


    public boolean checkUser(String userMd5) throws IOException {
        JSONObject jsonData = getJsonObject(FILENAME);
        if (jsonData == null) {
            return false;
        }
        for (String strKey : jsonData.keySet()) {
            JSONObject jsonUser = jsonData.getJSONObject(strKey);
            String id = jsonUser.getString("id");
            String pwd = jsonUser.getString("password");
            String strUsermd5 = SecureUtil.md5(String.format("%s:%s", id, pwd));
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
            DefaultSystemLog.LOG().error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 新增用户
     *
     * @param user 用户
     */
    public String addUser(UserInfoMode user) {
        if (user == null) {
            return JsonMessage.getString(400, "用户信息为空");
        }
        String password = user.getPassword();
        if (StrUtil.isEmpty(password)) {
            return JsonMessage.getString(400, "密码不能为空");
        }
        try {
            JSONObject jsonData = getJsonObject(FILENAME);
            String id = user.getId();
            if (jsonData.containsKey(id)) {
                return JsonMessage.getString(400, "该用户已存在");
            }
            JSONObject object = (JSONObject) JSON.toJSON(user);
            saveJson(FILENAME, object);
            return JsonMessage.getString(200, "添加成功");
        } catch (Exception e) {
            DefaultSystemLog.LOG().error(e.getMessage(), e);
        }
        return JsonMessage.getString(400, "添加失败");
    }

    /**
     * 修改用户
     *
     * @param user 用户
     * @return String
     */
    public String updateUser(UserInfoMode user) {
        if (user == null) {
            return JsonMessage.getString(400, "用户信息为空");
        }
        System.out.println(user.toString());
        String id = user.getId();
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "修改失败,获取id失败");
        }
        String name = StrUtil.nullToEmpty(user.getName());
        String password = StrUtil.nullToEmpty(user.getPassword());
        Boolean role = user.getRole();
        try {
            JSONObject jsonData = getJsonObject(FILENAME);
            JSONObject jsonObject = jsonData.getJSONObject(id);
            jsonObject.put("name", name);
            if (!StrUtil.isEmpty(password)) {
                jsonObject.put("password", password);
            }
            jsonObject.put("role", role);
            updateJson(FILENAME, jsonObject);
            return JsonMessage.getString(200, "修改成功");
        } catch (Exception e) {
            DefaultSystemLog.LOG().error(e.getMessage(), e);
        }
        return JsonMessage.getString(400, "修改失败");
    }
}
