package cn.jiangzeyin.service;

import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.util.JsonUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
     * @return
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
}
