package cn.keepbx.jpom.controller.user;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author jiangzeyin
 * @date 2019/4/21
 */
@Controller
@RequestMapping(value = "/user")
public class UserEditController extends BaseServerController {

    @Resource
    private UserService userService;

    @RequestMapping(value = "edit", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String edit(String id) {
        List<NodeModel> nodeModels = nodeService.listAndProject();
        //
        String reqId = nodeService.cacheNodeList(nodeModels);
        setAttribute("reqId", reqId);
        setAttribute("nodeModels", nodeModels);

        UserModel item = userService.getItem(id);
        setAttribute("userItem", item);
        return "user/edit";
    }
}
