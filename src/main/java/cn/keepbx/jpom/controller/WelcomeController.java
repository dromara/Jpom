package cn.keepbx.jpom.controller;

import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.UserModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 欢迎页
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/")
public class WelcomeController extends BaseController {

    @RequestMapping(value = "welcome", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        UserModel userName = getUser();
        setAttribute("userInfo", userName.getUserMd5Key());
        return "welcome";
    }
}
