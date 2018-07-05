package cn.jiangzeyin.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.controller.base.AbstractBaseControl;
import cn.jiangzeyin.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
@RequestMapping(value = "/")
public class IndexControl extends AbstractBaseControl {

    @Resource
    private UserService userService;

    @RequestMapping(value = "error", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String error() {
        return "error";
    }


    /**
     * 加载首页
     *
     * @return
     */
    @RequestMapping(value = "index", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
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

    /**
     * 修改密码
     *
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return
     */
    @RequestMapping(value = "updatePwd")
    @ResponseBody
    public String updatePwd(String oldPwd, String newPwd) {

        try {
            String result = userService.updatePwd(getSession().getAttribute(LoginInterceptor.SESSION_NAME).toString(), oldPwd, newPwd);

            // 用户不存在
            if ("notexist".equals(result)) {
                return JsonMessage.getString(500, "用户不存在！");
            }

            // 旧密码不正确
            if ("olderror".equals(result)) {
                return JsonMessage.getString(500, "旧密码不正确！");
            }

            // 如果修改成功，则销毁会话
            getSession().invalidate();
            return JsonMessage.getString(200, "修改密码成功！");
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error(e.getMessage(), e);
            return JsonMessage.getString(500, e.getMessage());
        }
    }
}
