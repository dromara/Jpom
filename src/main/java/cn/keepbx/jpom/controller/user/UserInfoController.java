package cn.keepbx.jpom.controller.user;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.ConfigBean;
import cn.keepbx.jpom.system.ExtConfigBean;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户管理
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
@RestController
@RequestMapping(value = "/user")
public class UserInfoController extends BaseController {
    @Resource
    private UserService userService;

    /**
     * 修改密码
     *
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return json
     */
    @RequestMapping(value = "updatePwd", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updatePwd(String oldPwd, String newPwd) {
        if (StrUtil.isEmpty(oldPwd) || StrUtil.isEmpty(newPwd) || oldPwd.length() < UserModel.USER_PWD_LEN || newPwd.length() < UserModel.USER_PWD_LEN) {
            return JsonMessage.getString(400, "密码长度为6-12位");
        }
        if (oldPwd.equals(newPwd)) {
            return JsonMessage.getString(400, "新旧密码一致");
        }
        UserModel userName = getUser();
        if (ConfigBean.getInstance().safeMode && UserModel.SYSTEM_ADMIN.equals(userName.getParent())) {
            return JsonMessage.getString(401, "安全模式下管理员的密码不能通过WEB端修改");
        }
        try {
            UserModel userModel = userService.login(userName.getId(), oldPwd);
            if (userModel == null || userModel.getPwdErrorCount() > 0) {
                return JsonMessage.getString(500, "旧密码不正确！");
            }
            userModel.setPassword(newPwd);
            if (!userService.updateUser(userModel)) {
                return JsonMessage.getString(500, "修改失败！");
            }
            // 如果修改成功，则销毁会话
            getSession().invalidate();
            return JsonMessage.getString(200, "修改密码成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }

    @RequestMapping(value = "updateName", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateName(String name) {
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(405, "请输入新的昵称");
        }
        int len = name.length();
        if (len > 10 || len < 2) {
            return JsonMessage.getString(405, "昵称长度只能是2-10");
        }
        UserModel userModel = getUser();
        userModel = userService.getItem(userModel.getId());
        userModel.setName(name);
        if (userService.updateUser(userModel)) {
            setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
            return JsonMessage.getString(200, "修改成功");
        }
        return JsonMessage.getString(500, "修改失败");
    }

    /**
     * 删除用户
     *
     * @param id 用户id
     * @return String
     */
    @RequestMapping(value = "deleteUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteUser(String id) {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你没有删除用户的权限");
        }
        if (userName.getId().equals(id)) {
            return JsonMessage.getString(400, "不能删除自己");
        }
        UserModel userModel = userService.getItem(id);
        if (UserModel.SYSTEM_ADMIN.equals(userModel.getParent())) {
            return JsonMessage.getString(400, "不能删除系统管理员");
        }
        boolean b = userService.deleteUser(id);
        if (b) {
            return JsonMessage.getString(200, "删除成功");
        }
        return JsonMessage.getString(400, "删除失败");
    }

    /**
     * 新增用户
     *
     * @param id       登录名
     * @param manage   是否为管理员
     * @param name     用户名
     * @param password 用户密码
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String addUser(String id, String name, String manage, String password) {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你还没有权限");
        }
        //
        int size = userService.userSize();
        if (size >= ExtConfigBean.getInstance().userMaxCount) {
            return JsonMessage.getString(500, "当前用户个数超过系统上限");
        }
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "登录名不能为空");
        }
        if (StrUtil.isEmpty(password)) {
            return JsonMessage.getString(400, "密码不能为空");
        }
        int length = password.length();
        if (length < UserModel.USER_PWD_LEN) {
            return JsonMessage.getString(400, "密码长度为6-12位");
        }
        boolean manageB = "true".equals(manage);

        UserModel userModel = userService.getItem(id);
        if (userModel != null) {
            return JsonMessage.getString(401, "登录名已经存在");
        }
        userModel = new UserModel();
        userModel.setName(name);
        userModel.setId(id);
        userModel.setPassword(password);
        userModel.setManage(manageB);
        userModel.setParent(userName.getId());

        String[] projects = getParameters("project");
        JSONArray jsonProjects = null;
        if (projects != null) {
            jsonProjects = (JSONArray) JSONArray.toJSON(projects);
        }

        userModel.setProjects(jsonProjects);

        boolean b = userService.addUser(userModel);
        if (b) {
            return JsonMessage.getString(200, "添加成功");
        }
        return JsonMessage.getString(400, "添加失败");
    }

    /**
     * 修改用户
     *
     * @param id       登录名
     * @param manage   是否为管理员
     * @param name     用户名
     * @param password 用户密码
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateUser(String id, String name, String manage, String password) {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你还没有权限");
        }

        String[] projects = getParameters("project");
        JSONArray jsonProjects = null;
        if (projects != null) {
            jsonProjects = (JSONArray) JSONArray.toJSON(projects);
        }
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(400, "修改失败:-1");
        }
        // 禁止修改系统管理员信息
        if (UserModel.SYSTEM_ADMIN.equals(userModel.getParent())) {
            return JsonMessage.getString(401, "WEB端不能修改系统管理员信息");
        }
        userModel.setName(name);
        if (!StrUtil.isEmpty(password)) {
            int length = password.length();
            if (length < UserModel.USER_PWD_LEN) {
                return JsonMessage.getString(400, "密码长度为6-12位");
            }
            //
            if (UserModel.SYSTEM_ADMIN.equals(userName.getParent())) {
                return JsonMessage.getString(401, "只有系统管理员才能重置用户密码");
            }
            userModel.setPassword(password);
        }

        boolean manageB = "true".equals(manage);

        if (!manageB && UserModel.SYSTEM_ADMIN.equals(userModel.getParent())) {
            return JsonMessage.getString(401, "不能取消系统管理员的管理权限");
        }
        userModel.setManage(manageB);

        userModel.setProjects(jsonProjects);
        boolean b = userService.updateUser(userModel);
        if (b) {
            return JsonMessage.getString(200, "修改成功");
        }
        return JsonMessage.getString(400, "修改失败");
    }

}
