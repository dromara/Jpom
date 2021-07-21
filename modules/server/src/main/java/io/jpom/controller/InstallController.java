package io.jpom.controller;

import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.model.data.UserModel;
import io.jpom.model.dto.UserLoginDto;
import io.jpom.service.user.UserService;
import io.jpom.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 初始化程序
 *
 * @author jiangzeyin
 * @date 2019/2/22
 */
@Controller
public class InstallController extends BaseServerController {
    @Resource
    private UserService userService;

//    @RequestMapping(value = "install.html", produces = MediaType.TEXT_HTML_VALUE)
//    @NotLogin
//    public String install() {
//        if (userService.userListEmpty()) {
//            return "install";
//        }
//        // 已存在用户跳转到首页
//        return BaseJpomInterceptor.getRedirect(getRequest(), "/index.html");
//    }

    /**
     * 初始化提交
     *
     * @param userName 系统管理员登录名
     * @param userPwd  系统管理员的登录密码
     * @return json
     */
    @RequestMapping(value = "install_submit.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @NotLogin
    @ResponseBody
    public String installSubmit(
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "登录名不能为空"),
                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, range = "3:20", msg = "登录名长度范围3-20"),
                    @ValidatorItem(value = ValidatorRule.WORD, msg = "登录名不能包含汉字并且不能包含特殊字符")
            }) String userName,
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空")
            }) String userPwd) {
        if (!userService.userListEmpty()) {
            return JsonMessage.getString(100, "系统已经初始化过啦，请勿重复初始化");
        }
        if (JpomApplication.SYSTEM_ID.equalsIgnoreCase(userName) || UserModel.SYSTEM_ADMIN.equals(userName)) {
            return JsonMessage.getString(400, "当前登录名已经被系统占用啦");
        }
        // 创建用户
        UserModel userModel = new UserModel();
        userModel.setName(UserModel.SYSTEM_OCCUPY_NAME);
        userModel.setId(userName);
        userModel.setPassword(userPwd);
        userModel.setParent(UserModel.SYSTEM_ADMIN);
        try {
            userService.addItem(userModel);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("初始化用户失败", e);
            return JsonMessage.getString(400, "初始化失败");
        }
        // 自动登录
        setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
        UserLoginDto userLoginDto = new UserLoginDto(userModel, JwtUtil.builder(userModel));
        return JsonMessage.getString(200, "初始化成功", userLoginDto);
    }
}
