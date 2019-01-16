package cn.jiangzeyin.controller.user;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


/**
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/user")
public class UserInfoController extends BaseController {
    @Resource
    private UserService userService;


    /**
     * 删除用户
     *
     * @param id 用户id
     * @return String
     */
    @RequestMapping(value = "deleteUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteUser(String id) {
        boolean b = userService.deleteUser(id);
        if (b) {
            return JsonMessage.getString(200, "删除成功");
        }
        return JsonMessage.getString(400, "删除失败");
    }

    /**
     * 新增用户
     *
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String addUser(String id, String name, String role, String password) {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "登录名不能为空");
        }
        if (StrUtil.isEmpty(password)) {
            return JsonMessage.getString(400, "密码不能为空");
        }
        int length = password.length();
        if (length < 6) {
            return JsonMessage.getString(400, "密码长度为6-12位");
        }
        boolean b = userService.addUser(id, name, password, role);
        if (b) {
            return JsonMessage.getString(200, "添加成功");
        }
        return JsonMessage.getString(400, "添加失败");
    }

    /**
     * 修改用户
     *
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateUser(String id, String name, String role, String password) {
        if (StrUtil.isEmpty(id)) {
            return JsonMessage.getString(400, "登录名不能为空");
        }
        if (!StrUtil.isEmpty(password)) {
            int length = password.length();
            if (length < 6) {
                return JsonMessage.getString(400, "密码长度为6-12位");
            }
        }
        boolean b = userService.updateUser(id, name, password, role);
        if (b) {
            return JsonMessage.getString(200, "修改成功");
        }
        return JsonMessage.getString(400, "修改失败");
    }

}
