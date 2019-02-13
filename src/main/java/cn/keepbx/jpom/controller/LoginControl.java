package cn.keepbx.jpom.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.NotLogin;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * @author Administrator
 */
@Controller
public class LoginControl extends BaseController {

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
            UserModel userModel = userService.login(userName, userPwd);
            if (userModel != null) {
                stringBuffer.append("，结果：").append("OK");
                setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
//                setSessionAttribute(LoginInterceptor.SESSION_PWD, userPwd);
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
