package cn.keepbx.jpom.controller;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.common.GlobalDefaultExceptionHandler;
import cn.keepbx.jpom.common.interceptor.NotLogin;
import cn.keepbx.jpom.model.system.JpomManifest;
import cn.keepbx.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;

/**
 * 首页
 *
 * @author Administrator
 */
@Controller
@RequestMapping(value = "/")
public class IndexControl extends BaseServerController {
    @Resource
    private UserService userService;

    @RequestMapping(value = "error", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public String error(String id) {
        String msg = GlobalDefaultExceptionHandler.getErrorMsg(id);
        setAttribute("msg", msg);
        return "error";
    }

    /**
     * 加载首页
     *
     * @return page
     */
    @RequestMapping(value = {"index", "", "index.html", "/"}, method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        if (userService.userListEmpty()) {
            getSession().invalidate();
            return "redirect:install.html";
        }
        // 版本号
        setAttribute("jpomManifest", JpomManifest.getInstance());
        return "index";
    }


}
