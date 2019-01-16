package cn.jiangzeyin.controller.user;

import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.controller.BaseController;
import cn.jiangzeyin.service.UserService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/user")
public class UserListController extends BaseController {

    @Resource
    private UserService userService;

    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list")
    public String projectInfo() {
        return "user/list";
    }

    /**
     * 查询所有用户
     */
    @RequestMapping(value = "getUserList")
    @ResponseBody
    public String getUserList() {
        JSONArray userList = userService.getUserList();
        return JsonMessage.getString(200, "", userList);
    }


}
