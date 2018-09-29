package cn.jiangzeyin.controller.user;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.model.UserInfoMode;
import cn.jiangzeyin.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


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
     * @param user 用户
     * @return String
     */
    @RequestMapping(value = "addUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String addUser(UserInfoMode user) {
        return userService.addUser(user);
    }

    /**
     * 修改用户
     *
     * @param user 用户
     * @return String
     */
    @RequestMapping(value = "updateUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String updateUser(UserInfoMode user) {
        return userService.updateUser(user);
    }

}
