package cn.keepbx.jpom.controller.user;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.controller.BaseController;
import cn.keepbx.jpom.service.UserService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
    @RequestMapping(value = "list", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        return "user/list";
    }

    /**
     * 查询所有用户
     */
    @RequestMapping(value = "getUserList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getUserList() {
        boolean manager = userService.isManager("", getUserName());
        if (!manager) {
            return JsonMessage.getString(400, "你没有对应权限！");
        }
        JSONArray userList = userService.getUserList();
        return JsonMessage.getString(200, "", userList);
    }


}
