package cn.keepbx.jpom.controller.user;

import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.ProjectInfoModel;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Iterator;
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
    private ProjectInfoService projectInfoService;

    /**
     * 展示用户列表
     */
    @RequestMapping(value = "list", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String projectInfo() {
        List<ProjectInfoModel> jsonArray = projectInfoService.list();
        setAttribute("projects", jsonArray);
        return "user/list";
    }

    /**
     * 查询所有用户
     */
    @RequestMapping(value = "getUserList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String getUserList() {
        UserModel userName = getUser();
        if (!userName.isManage()) {
            return JsonMessage.getString(400, "你没有对应权限！");
        }
        List<UserModel> userList = userService.list();
        if (userList != null) {
            Iterator<UserModel> userModelIterator = userList.iterator();
            // 不显示自己的信息
            while (userModelIterator.hasNext()) {
                UserModel item = userModelIterator.next();
                if (item.getId().equals(userName.getId())) {
                    userModelIterator.remove();
                    break;
                }
            }
        }
        return JsonMessage.getString(200, "", userList);
    }
}
