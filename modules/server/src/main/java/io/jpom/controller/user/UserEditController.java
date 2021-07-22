package io.jpom.controller.user;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.user.RoleService;
import io.jpom.service.user.UserService;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Controller
@RequestMapping(value = "/user")
@Feature(cls = ClassFeature.USER)
public class UserEditController extends BaseServerController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

//    @RequestMapping(value = "edit", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String edit(String id) {
//        UserModel item = userService.getItem(id);
//        if (item != null) {
//            item.setPassword(null);
//            setAttribute("userItem", item);
//        }
//        List<RoleModel> list = roleService.list();
//        JSONArray roles = new JSONArray();
//        list.forEach(userModel -> {
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("title", userModel.getName());
//            jsonObject.put("value", userModel.getId());
//            roles.add(jsonObject);
//        });
//        setAttribute("roles", roles);
//        return "user/edit";
//    }

    /**
     * 新增用户
     *
     * @param id 登录名
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.AddUer)
    @Feature(method = MethodFeature.EDIT)
    @ResponseBody
    public String addUser(String id) {
        if (JpomApplication.SYSTEM_ID.equalsIgnoreCase(id)) {
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
        //
        String roles = getParameter("roles");
        JSONArray jsonArray = JSONArray.parseArray(roles);
        List<String> rolesList = jsonArray.toJavaList(String.class);
        if (rolesList == null || rolesList.isEmpty()) {
            return JsonMessage.getString(405, "请选择一个角色");
        }
        userModel.setRoles(rolesList);
        return null;
    }

    /**
     * 修改用户
     *
     * @param id 登录名
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @OptLog(UserOperateLogV1.OptType.EditUer)
    @Feature(method = MethodFeature.EDIT)
    @ResponseBody
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
        userService.updateItem(userModel);
        return JsonMessage.getString(200, "修改成功");
    }
}
