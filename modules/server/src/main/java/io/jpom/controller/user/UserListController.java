package io.jpom.controller.user;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.user.RoleService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户列表
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/user")
@Feature(cls = ClassFeature.USER)
public class UserListController extends BaseServerController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

//    /**
//     * 展示用户列表
//     *
//     * @return page
//     */
//    @RequestMapping(value = "list", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.LIST)
//    public String list() {
//        List<RoleModel> roleModels = roleService.list();
//        setAttribute("roleEmpty", roleModels == null || roleModels.isEmpty());
//        return "user/list";
//    }


    /**
     * 查询所有用户
     *
     * @return json
     */
    @RequestMapping(value = "getUserList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String getUserList() {
        UserModel userName = getUser();
        List<UserModel> userList = userService.list();
        if (userList != null) {
            userList = userList.stream().filter(userModel -> {
                // 不显示自己的信息
                return !userModel.getId().equals(userName.getId());
            }).collect(Collectors.toList());
        }
        return JsonMessage.getString(200, "", userList);
    }

    /**
     * @return
     * @author Hotstrip
     * get all admin user list
     * 获取所有管理员信息
     */
    @RequestMapping(value = "get-admin-user-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @Feature(method = MethodFeature.LIST)
    public String getAdminUserList() {
        List<UserModel> list = userService.list(false);
        JSONArray jsonArray = new JSONArray();
        list.forEach(userModel -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", userModel.getName());
            jsonObject.put("value", userModel.getId());
            if (StrUtil.isAllEmpty(userModel.getEmail(), userModel.getDingDing(), userModel.getWorkWx())) {
                jsonObject.put("disabled", true);
            }
            jsonArray.add(jsonObject);
        });
        return JsonMessage.getString(200, "success", jsonArray);
    }
}
