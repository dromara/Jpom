package cn.keepbx.jpom.service.user;

import cn.keepbx.jpom.common.BaseOperService;
import cn.keepbx.jpom.model.data.RoleModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.system.ServerConfigBean;
import cn.keepbx.permission.DynamicData;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.MethodFeature;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理
 *
 * @author Administrator
 */
@Service
public class UserService extends BaseOperService<UserModel> {

    @Resource
    private RoleService roleService;

    public UserService() {
        super(ServerConfigBean.USER);
    }

    /**
     * 用户列表是否为空
     *
     * @return true 为空需要初始化
     */
    public boolean userListEmpty() {
        return userSize() <= 0;
    }

    /**
     * 获取系统用户个数
     *
     * @return int
     */
    public int userSize() {
        try {
            JSONObject userInfo = getJSONObject(ServerConfigBean.USER);
            if (userInfo == null) {
                return 0;
            }
            return userInfo.keySet().size();
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * 用户登录
     *
     * @param name 用户名
     * @param pwd  密码
     * @return 登录
     */
    public UserModel simpleLogin(String name, String pwd) throws IOException {
        UserModel userModel = getItem(name);
        if (userModel == null) {
            return null;
        }
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
     */
    public UserModel checkUser(String userMd5) {
        JSONObject jsonData = getJSONObject(ServerConfigBean.USER);
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
    @Override
    public List<UserModel> list() {
        return list(true);
    }

    public boolean errorDynamicPermission(UserModel userModel, ClassFeature classFeature, String dataId) {
        DynamicData dynamicData1 = DynamicData.getDynamicDataMap().get(classFeature);
        if (dynamicData1 == null) {
            // 如果不是没有动态权限  就默认通过
            return false;
        }
        List<String> roles = userModel.getRoles();
        if (roles == null || roles.isEmpty()) {
            return true;
        }
        for (String role : roles) {
            RoleModel item = roleService.getItem(role);
            if (item == null) {
                continue;
            }
            Map<ClassFeature, List<String>> dynamicData = item.getDynamicData();
            if (dynamicData == null) {
                continue;
            }
            List<String> list = dynamicData.get(classFeature);
            if (list.contains(dataId)) {
                return false;
            }
        }
        return true;
    }

    public boolean errorMethodPermission(UserModel userModel, ClassFeature classFeature, MethodFeature methodFeature) {
        List<String> roles = userModel.getRoles();
        if (roles == null || roles.isEmpty()) {
            return true;
        }
        for (String role : roles) {
            RoleModel item = roleService.getItem(role);
            if (item == null) {
                continue;
            }
            List<MethodFeature> methodFeatures = item.getMethodFeature(classFeature);
            if (methodFeatures == null) {
                continue;
            }
            if (methodFeatures.contains(methodFeature)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否返回系统管理员信息
     *
     * @param system 系统管理员
     * @return list
     */
    public List<UserModel> list(boolean system) {
        List<UserModel> list = super.list();
        if (list == null) {
            return null;
        }
        return list.stream().filter(userModel -> {
            userModel.setPassword(null);
            // 不显示系统管理员信息
            return !system || !userModel.isSystemUser();
        }).collect(Collectors.toList());
    }
}
