package cn.keepbx.jpom.controller.user;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.ExtConfigBean;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;

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
        if (StrUtil.isEmpty(oldPwd) || StrUtil.isEmpty(newPwd)) {
            return JsonMessage.getString(400, "密码不能为空");
        }
        if (oldPwd.equals(newPwd)) {
            return JsonMessage.getString(400, "新旧密码一致");
        }
        UserModel userName = getUser();
//        if (UserModel.SYSTEM_ADMIN.equals(userName.getParent()) && CheckPassword.checkPassword(newPwd) != 2) {
//            return JsonMessage.getString(401, "系统管理员密码强度太低,请使用复杂的密码");
//        }
        try {
            UserModel userModel = userService.simpleLogin(userName.getId(), oldPwd);
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

    /**
     * 修改用户昵称
     *
     * @param name 新昵称
     * @return json
     */
    @RequestMapping(value = "updateName", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateName(String name) throws IOException {
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
    public String deleteUser(String id) throws IOException {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你没有删除用户的权限");
        }
        if (ExtConfigBean.getInstance().safeMode) {
            return JsonMessage.getString(401, "安全模式不能删除用户");
        }
        if (userName.getId().equals(id)) {
            return JsonMessage.getString(400, "不能删除自己");
        }
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(501, "非法访问");
        }
        if (userModel.isSystemUser()) {
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
     * @param id 登录名
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String addUser(String id) throws IOException {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你还没有权限");
        }
        //
        int size = userService.userSize();
        if (size >= ExtConfigBean.getInstance().userMaxCount) {
            return JsonMessage.getString(500, "当前用户个数超过系统上限");
        }

        UserModel userModel = userService.getItem(id);
        if (userModel != null) {
            return JsonMessage.getString(401, "登录名已经存在");
        }
        userModel = new UserModel();
        userModel.setParent(userName.getId());
        String msg = parseUser(userModel, true);
        if (msg != null) {
            return msg;
        }
        userService.addItem(userModel);
        return JsonMessage.getString(200, "添加成功");
    }

    private String parseUser(UserModel userModel, boolean create) {
        String id = getParameter("id");
        if (StrUtil.isEmpty(id) || id.length() < UserModel.USER_NAME_MIN_LEN) {
            return JsonMessage.getString(400, "登录名不能为空,并且长度必须不小于" + UserModel.USER_NAME_MIN_LEN);
        }
        if (UserModel.SYSTEM_OCCUPY_NAME.equals(id) || UserModel.SYSTEM_ADMIN.equals(id)) {
            return JsonMessage.getString(401, "当前登录名已经被系统占用");
        }
        if (!checkPathSafe(id)) {
            return JsonMessage.getString(400, "登录名不能包含特殊字符");
        }
        userModel.setId(id);

        String name = getParameter("name");
        if (StrUtil.isEmpty(name)) {
            return JsonMessage.getString(405, "请输入账户昵称");
        }
        int len = name.length();
        if (len > 10 || len < 2) {
            return JsonMessage.getString(405, "昵称长度只能是2-10");
        }
        userModel.setName(name);

        UserModel userName = getUser();
        String password = getParameter("password");
        if (create || StrUtil.isNotEmpty(password)) {
            if (StrUtil.isEmpty(password)) {
                return JsonMessage.getString(400, "密码不能为空");
            }
            // 修改用户
            if (!create && !userName.isSystemUser()) {
                return JsonMessage.getString(401, "只有系统管理员才能重置用户密码");
            }
            userModel.setPassword(password);
        }

        String projects = getParameter("project");
        JSONArray jsonProjects = null;
        if (projects != null) {
            jsonProjects = JSONArray.parseArray(projects);
        }
        userModel.setProjects(jsonProjects);

        boolean manageB = "true".equals(getParameter("manage"));
        userModel.setManage(manageB);

        manageB = "true".equals(getParameter("uploadFile"));
        // 如果操作人没有权限  就不能管理被操作者
        if (!userName.isUploadFile() && manageB) {
            return JsonMessage.getString(402, "你没有管理上传文件的权限");
        }
        userModel.setUploadFile(manageB);

        manageB = "true".equals(getParameter("deleteFile"));
        if (!userName.isDeleteFile() && manageB) {
            return JsonMessage.getString(402, "你没有管理删除文件的权限");
        }
        userModel.setDeleteFile(manageB);
        return null;
    }

    /**
     * 修改用户
     *
     * @param id 登录名
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateUser(String id) throws IOException {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你还没有权限");
        }
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(400, "修改失败:-1");
        }
        // 禁止修改系统管理员信息
        if (userModel.isSystemUser()) {
            return JsonMessage.getString(401, "WEB端不能修改系统管理员信息");
        }
        String msg = parseUser(userModel, false);
        if (msg != null) {
            return msg;
        }
        boolean b = userService.updateUser(userModel);
        if (b) {
            return JsonMessage.getString(200, "修改成功");
        }
        return JsonMessage.getString(400, "修改失败");
    }

    /**
     * 解锁用户锁定状态
     *
     * @param id id
     * @return json
     * @throws IOException io
     */
    @RequestMapping(value = "unlock", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String unlock(String id) throws IOException {
        UserModel userName = getUser();
        if (!userName.isSystemUser()) {
            return JsonMessage.getString(400, "你还没有权限");
        }
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(400, "修改失败:-1");
        }
        userModel.unLock();
        boolean b = userService.updateUser(userModel);
        if (b) {
            return JsonMessage.getString(200, "解锁成功");
        }
        return JsonMessage.getString(400, "解锁失败");
    }
}
