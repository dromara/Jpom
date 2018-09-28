package cn.jiangzeyin.controller;

import cn.jiangzeyin.controller.base.AbstractBaseControl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/")
public class IndexControl extends AbstractBaseControl {

    @RequestMapping(value = "error", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String error() {
        return "error";
    }

    /**
     * 加载首页
     *
     * @return
     */
    @RequestMapping(value = {"index", "", "index.html", "/"}, method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return "index";
    }

    @RequestMapping(value = "welcome")
    public String welcome() {
        return "welcome";
    }

    /**
     * 退出登录
     *
     * @return
     */
    @RequestMapping(value = "logout")
    public String logout() {
        getSession().invalidate();
        return "login";
    }
}
