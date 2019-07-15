package cn.keepbx.jpom.controller.user;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.jpom.BaseJpomApplication;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.UrlPermission;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.service.manage.TomcatService;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.ServerExtConfigBean;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户管理
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
@RestController
@RequestMapping(value = "/user")
public class UserInfoController extends BaseServerController {
    @Resource
    private UserService userService;

    @Resource
    private TomcatService tomcatService;

    /**
     * 修改密码
     *
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return json
     */
    @RequestMapping(value = "updatePwd", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updatePwd(
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空")
            }) String oldPwd,
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空")
            }) String newPwd) {
        if (oldPwd.equals(newPwd)) {
            return JsonMessage.getString(400, "新旧密码一致");
        }
        UserModel userName = getUser();
        if (userName.isDemoUser()) {
            return JsonMessage.getString(402, "当前账户为演示账号，不支持修改密码");
        }
        try {
            UserModel userModel = userService.simpleLogin(userName.getId(), oldPwd);
            if (userModel == null || userModel.getPwdErrorCount() > 0) {
                return JsonMessage.getString(500, "旧密码不正确！");
            }
            userModel.setPassword(newPwd);
            if (!userService.updateItem(userModel)) {
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
    public String updateName(@ValidatorConfig(value = {
            @ValidatorItem(value = ValidatorRule.NOT_BLANK, range = "2:10", msg = "昵称长度只能是2-10")
    }) String name) {
        UserModel userModel = getUser();
        userModel = userService.getItem(userModel.getId());
        userModel.setName(name);
        if (userService.updateItem(userModel)) {
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
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.DelUer)
    public String deleteUser(String id) {
        UserModel userName = getUser();
        if (userName.getId().equals(id)) {
            return JsonMessage.getString(400, "不能删除自己");
        }
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(501, "非法访问");
        }
        // 非系统管理员不支持删除演示账号
        if (!userName.isSystemUser() && userModel.isDemoUser()) {
            return JsonMessage.getString(402, "演示账号不支持删除");
        }
        if (userModel.isSystemUser()) {
            return JsonMessage.getString(400, "非法访问:-5");
        }
        userService.deleteItem(id);
        return JsonMessage.getString(200, "删除成功");
    }

    /**
     * 新增用户
     *
     * @param id 登录名
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.AddUer)
    public String addUser(String id) {
        if (BaseJpomApplication.SYSTEM_ID.equalsIgnoreCase(id)) {
            return JsonMessage.getString(400, "当前登录名已经被系统占用啦");
        }
        UserModel userName = getUser();
        //
        int size = userService.userSize();
        if (size >= ServerExtConfigBean.getInstance().userMaxCount) {
            return JsonMessage.getString(500, "当前用户个数超过系统上限");
        }

        UserModel userModel = userService.getItem(id);
        if (userModel != null) {
            return JsonMessage.getString(401, "登录名已经存在");
        }
        userModel = new UserModel();
        // 隐藏系统管理员登录名
        if (userName.isSystemUser()) {
            userModel.setParent(UserModel.SYSTEM_OCCUPY_NAME);
        } else {
            userModel.setParent(userName.getId());
        }
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
        return parseRole(userName, userModel);
    }

    private String parseRole(UserModel optUser, UserModel userModel) {
        String reqId = getParameter("reqId");
        List<NodeModel> list = nodeService.getNodeModel(reqId);
        if (list == null) {
            return JsonMessage.getString(401, "页面请求超时");
        }
        // 服务管理员
        boolean manageB = "true".equals(getParameter("serverManager"));
        if (!optUser.isSystemUser() && manageB) {
            return JsonMessage.getString(402, "你不是系统管理员不能创建服务管理员");
        }
        userModel.setServerManager(manageB);

        UserModel.NodeRole nodeRole;
        UserModel.NodeRole nodeTomcatRole;
        String projects = getParameter("projects");
        JSONObject projectsJson = null;
        if (StrUtil.isNotEmpty(projects)) {
            projectsJson = JSONObject.parseObject(projects);
        }
        for (NodeModel nodeModel : list) {
            nodeRole = new UserModel.NodeRole();
            nodeTomcatRole = new UserModel.NodeRole();
            nodeRole.setId(nodeModel.getId());
            nodeTomcatRole.setId(nodeModel.getId());

            manageB = "true".equals(getParameter(StrUtil.format("{}_manage", nodeRole.getId())));
            nodeRole.setManage(manageB);
            nodeTomcatRole.setManage(manageB);

            manageB = "true".equals(getParameter(StrUtil.format("{}_uploadFile", nodeRole.getId())));
            // 如果操作人没有权限  就不能管理被操作者
            if (!optUser.isUploadFile(nodeModel.getId()) && manageB) {
                return JsonMessage.getString(402, "你没有管理上传文件的权限");
            }
            nodeRole.setUploadFile(manageB);

            manageB = "true".equals(getParameter(StrUtil.format("{}_uploadTomcatFile", nodeRole.getId())));
            if (!optUser.isUploadTomcatFile(nodeModel.getId()) && manageB) {
                return JsonMessage.getString(402, "你没有管理上传Tomcat文件的权限");
            }
            nodeTomcatRole.setUploadFile(manageB);

            manageB = "true".equals(getParameter(StrUtil.format("{}_deleteFile", nodeRole.getId())));
            if (!optUser.isDeleteFile(nodeModel.getId()) && manageB) {
                return JsonMessage.getString(402, "你没有管理删除文件的权限");
            }
            nodeRole.setDeleteFile(manageB);

            manageB = "true".equals(getParameter(StrUtil.format("{}_deleteTomcatFile", nodeRole.getId())));
            if (!optUser.isDeleteTomcatFile(nodeModel.getId()) && manageB) {
                return JsonMessage.getString(402, "你没有管理删除Tomcat文件的权限");
            }
            nodeTomcatRole.setDeleteFile(manageB);

            JSONArray jsonArray = nodeModel.getProjects();
            if (jsonArray != null && projectsJson != null) {
                JSONArray jsonArray1 = projectsJson.getJSONArray(nodeModel.getId());
                nodeRole.setProjects(jsonArray1);
            }
            JSONArray tomcatList = tomcatService.getTomcatList(nodeModel);
            if (tomcatList != null && tomcatList.size() > 0) {
                JSONArray jsonArray1 = new JSONArray();
                tomcatList.forEach(o -> {
                    JSONObject data = (JSONObject) o;
                    String id1 = data.getString("id");
                    String val = getParameter(StrUtil.format("p_{}_{}", nodeModel.getId(), id1));
                    if (id1.equals(val)) {
                        jsonArray1.add(id1);
                    }
                });
                nodeTomcatRole.setProjects(jsonArray1);
            }
            userModel.putNodeRole(nodeRole);
            userModel.putNodeTomcatRole(nodeTomcatRole);
        }
        return null;
    }

    /**
     * 修改用户
     *
     * @param id 登录名
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.ServerManager, optType = UserOperateLogV1.OptType.EditUer)
    public String updateUser(String id) {
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(400, "修改失败:-1");
        }
        // 禁止修改系统管理员信息
        if (userModel.isSystemUser()) {
            return JsonMessage.getString(401, "WEB端不能修改系统管理员信息");
        }
        UserModel me = getUser();
        if (userModel.getId().equals(me.getId())) {
            return JsonMessage.getString(401, "不能修改自己的信息");
        }
        // 非系统管理员不能修改演示账号信息
        if (!me.isSystemUser() && userModel.isDemoUser()) {
            return JsonMessage.getString(402, "不支持修改演示账号信息");
        }
        String msg = parseUser(userModel, false);
        if (msg != null) {
            return msg;
        }
        // 记录修改时间，如果在线用户线退出
        userModel.setModifyTime(DateUtil.currentSeconds());
        boolean b = userService.updateItem(userModel);
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
     */
    @RequestMapping(value = "unlock", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @UrlPermission(value = Role.System, optType = UserOperateLogV1.OptType.UnlockUer)
    public String unlock(String id) {
        UserModel userModel = userService.getItem(id);
        if (userModel == null) {
            return JsonMessage.getString(400, "修改失败:-1");
        }
        userModel.unLock();
        boolean b = userService.updateItem(userModel);
        if (b) {
            return JsonMessage.getString(200, "解锁成功");
        }
        return JsonMessage.getString(400, "解锁失败");
    }
}
