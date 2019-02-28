package cn.keepbx.jpom.controller;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.NotLogin;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 初始化程序
 *
 * @author jiangzeyin
 * @date 2019/2/22
 */
@Controller
public class InstallController extends BaseController {
    @Resource
    private UserService userService;

    @RequestMapping(value = "install.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public String install() throws IOException {
        if (userService.userListEmpty()) {
            return "install";
        }
        // 已存在用户跳转到首页
        return "redirect:index.html";
    }

    /**
     * 初始化提交
     *
     * @return json
     */
    @RequestMapping(value = "install_submit.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @NotLogin
    @ResponseBody
    public String installSubmit(String userName, String userPwd) {
        if (StrUtil.isEmpty(userName)) {
            return JsonMessage.getString(400, "登录名不能为空");
        }
        if (Validator.isChinese(userName)) {
            return JsonMessage.getString(400, "登录名不能包含汉字");
        }
        if (StrUtil.isEmpty(userPwd)) {
            return JsonMessage.getString(400, "密码不能为空");
        }
        int length = userPwd.length();
        if (length < 6) {
            return JsonMessage.getString(400, "密码长度为6-12位");
        }
        UserModel userModel = new UserModel();
        userModel.setName("超级管理员");
        userModel.setId(userName);
        userModel.setPassword(userPwd);
        userModel.setManage(true);
        boolean b = userService.addUser(userModel);
        if (b) {
            // 自动登录
            setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
            return JsonMessage.getString(200, "初始化成功");
        }
        return JsonMessage.getString(400, "初始化失败");
    }
}
