package cn.jiangzeyin.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.common.interceptor.NotLogin;
import cn.jiangzeyin.controller.base.AbstractBaseControl;
import cn.jiangzeyin.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
public class LoginControl extends AbstractBaseControl {

    @Resource
    private UserService userService;

    @RequestMapping(value = {"login.html", "", "/"})
    @NotLogin
    public String login() {
        return "login";
    }

    @RequestMapping(value = "userLogin", method = RequestMethod.POST)
    @ResponseBody
    @NotLogin
    public String userLogin(String userName, String userPwd) {
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("用户登录：").append(userName).append(",IP：").append(getIp());
        stringBuffer.append(",浏览器：").append(getHeader(HttpHeaders.USER_AGENT));
        try {
            boolean flag = userService.login(userName, userPwd);
            if (flag) {
                stringBuffer.append("，结果：").append("OK");
                getSession().setAttribute(LoginInterceptor.SESSION_NAME, userName);
                getSession().setAttribute(LoginInterceptor.SESSION_PWD, userPwd);
                return JsonMessage.getString(200, "登录成功");
            } else {
                stringBuffer.append("，结果：").append("faild");
            }
            return JsonMessage.getString(400, "登录失败，请输入正确的密码和账号");
        } catch (Exception e) {
            stringBuffer.append("，结果：").append("error");
            return JsonMessage.getString(500, e.getLocalizedMessage());
        } finally {
            DefaultSystemLog.LOG().info(stringBuffer.toString());
        }
    }
}
