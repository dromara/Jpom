package cn.keepbx.jpom.controller;

import cn.keepbx.jpom.common.BaseController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Controller
public class WelcomeController extends BaseController {

    @RequestMapping(value = "welcome", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String welcome() {
        return "welcome";
    }
}
