package cn.keepbx.jpom.controller.user;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.ManageService;
import cn.keepbx.jpom.service.user.UserService;
import com.alibaba.fastjson.JSONArray;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/user")
public class UserListController extends BaseController {

    @Resource
    private UserService userService;

    @Resource
    private ManageService manageService;

    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() throws IOException {
        List<ProjectInfoModel> jsonArray = manageService.getAllProjectArrayInfo();
        setAttribute("projects", jsonArray);
        return "user/list";
    }

    /**
     * 查询所有用户
     */
    @RequestMapping(value = "getUserList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getUserList() {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你没有对应权限！");
        }
        JSONArray userList = userService.getUserList();
        return JsonMessage.getString(200, "", userList);
    }


}
