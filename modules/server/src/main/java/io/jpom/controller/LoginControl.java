package io.jpom.controller;

import cn.hutool.cache.impl.LFUCache;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.interceptor.LoginInterceptor;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.UserModel;
import io.jpom.model.dto.UserLoginDto;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.service.user.UserService;
import io.jpom.system.ServerConfigBean;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 登录控制
 *
 * @author Administrator
 */
@Controller
public class LoginControl extends BaseServerController {
    /**
     * ip 黑名单
     */
    public static final LFUCache<String, Integer> LFU_CACHE = new LFUCache<>(1000);

    private static final String LOGIN_CODE = "login_code";

    private static final String SHOW_CODE = "show_code";

//    public static final int INPUT_CODE = 600;
//    private static final int INPUT_CODE_ERROR_COUNT = 3;

    @Resource
    private UserService userService;

//    /**
//     * 登录页面
//     *
//     * @return login
//     */
//    @RequestMapping(value = "login.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @NotLogin
//    public String login() {
//        if (userService.userListEmpty()) {
//            // 调整到初始化也
//            return BaseJpomInterceptor.getRedirect(getRequest(), "/install.html");
//        }
//        // 是否显示验证码
//        setAttribute("showCode", showCode());
//        return "login";
//    }

//    private boolean showCode() {
//        String showCode = getSessionAttribute(SHOW_CODE);
//        return StrUtil.isNotEmpty(showCode);
//    }

    /**
     * 验证码
     *
     * @throws IOException IO
     */
    @RequestMapping(value = "randCode.png", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @NotLogin
    public void randCode() throws IOException {
        int height = 50;
        CircleCaptcha circleCaptcha = new CircleCaptcha(100, height, 4, 8);
        // 设置为默认字体
        circleCaptcha.setFont(new Font(null, Font.PLAIN, (int) (height * 0.75)));
        circleCaptcha.createCode();
        HttpServletResponse response = getResponse();
        circleCaptcha.write(response.getOutputStream());
        String code = circleCaptcha.getCode();
        setSessionAttribute(LOGIN_CODE, code);
        // 会话显示验证码
        setSessionAttribute(SHOW_CODE, true);
    }

    private Integer ipError() {
        if (ServerExtConfigBean.getInstance().getIpErrorLockTime() <= 0) {
            return 0;
        }
        String ip = getIp();
        int count = ObjectUtil.defaultIfNull(LFU_CACHE.get(ip), 0) + 1;
        LFU_CACHE.put(ip, count, ServerExtConfigBean.getInstance().getIpErrorLockTime());
        return count;
    }

    private void ipSuccess() {
        String ip = getIp();
        LFU_CACHE.remove(ip);
    }

    /**
     * 当登录的ip 错误次数达到配置的10倍以上锁定当前ip
     *
     * @return true
     */
    private boolean ipLock() {
        if (ServerExtConfigBean.getInstance().userAlwaysLoginError <= 0) {
            return false;
        }
        String ip = getIp();
        Integer count = LFU_CACHE.get(ip);
        if (count == null) {
            count = 0;
        }
        // 大于10倍时 封ip
        return count > ServerExtConfigBean.getInstance().userAlwaysLoginError * 10;
    }

    /**
     * 登录接口
     *
     * @param userName 登录名
     * @param userPwd  登录密码
     * @param code     验证码
     * @return json
     */
    @RequestMapping(value = "userLogin", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @NotLogin
    @OptLog(UserOperateLogV1.OptType.Login)
    public String userLogin(
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "请输入登录信息")
            }) String userName,
            @ValidatorConfig(value = {
                    @ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "请输入登录信息")
            }) String userPwd,
            String code) {
        if (this.ipLock()) {
            return JsonMessage.getString(400, "尝试次数太多，请稍后再来");
        }
        synchronized (UserModel.class) {
            UserModel userModel = userService.getItem(userName);
            if (userModel == null) {
                this.ipError();
                return JsonMessage.getString(400, "登录失败，请输入正确的密码和账号,多次失败将锁定账号");
            }
            // 获取验证码
            String sCode = getSessionAttribute(LOGIN_CODE);
            if (StrUtil.isEmpty(code) || !sCode.equalsIgnoreCase(code)) {
                return JsonMessage.getString(400, "请输入正确的验证码");
            }
            removeSessionAttribute(LOGIN_CODE);

            try {
                long lockTime = userModel.overLockTime();
                if (lockTime > 0) {
                    String msg = DateUtil.formatBetween(lockTime * 1000, BetweenFormatter.Level.MINUTE);
                    userModel.errorLock();
                    this.ipError();
                    return JsonMessage.getString(400, "该账户登录失败次数过多，已被锁定" + msg + ",请不要再次尝试");
                }
                // 验证
                if (userPwd.equals(userModel.getPassword())) {
                    userModel.unLock();
                    setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
                    removeSessionAttribute(SHOW_CODE);
                    this.ipSuccess();
                    UserLoginDto userLoginDto = new UserLoginDto(userModel, JwtUtil.builder(userModel));
                    return JsonMessage.getString(200, "登录成功", userLoginDto);
                } else {
                    userModel.errorLock();
                    this.ipError();
                    return JsonMessage.getString(501, "登录失败，请输入正确的密码和账号,多次失败将锁定账号");
                }
            } finally {
                userService.updateItem(userModel);
            }
        }
    }

//    /**
//     * 退出登录
//     *
//     * @return page
//     */
//    @RequestMapping(value = "logout", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    public String logoutPage() {
//        getSession().invalidate();
//        return BaseJpomInterceptor.getRedirect(getRequest(), "/old.html");
//    }

    /**
     * 退出登录
     *
     * @return json
     */
    @RequestMapping(value = "logout2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @NotLogin
    public String logout() {
        getSession().invalidate();
        return JsonMessage.getString(200, "退出成功");
    }

    /**
     * 刷新token
     */
    @RequestMapping(value = "renewal", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @NotLogin
    public String renewalToken() {
        String token = getRequest().getHeader(ServerOpenApi.HTTP_HEAD_AUTHORIZATION);
        if (StrUtil.isEmpty(token)) {
            return JsonMessage.getString(ServerConfigBean.AUTHORIZE_TIME_OUT_CODE, "刷新token失败");
        }
        JWT jwt = JwtUtil.readBody(token);
        if (JwtUtil.expired(jwt, 0)) {
            int renewal = ServerExtConfigBean.getInstance().getAuthorizeRenewal();
            if (jwt == null || renewal <= 0 || JwtUtil.expired(jwt, TimeUnit.MINUTES.toSeconds(renewal))) {
                return JsonMessage.getString(ServerConfigBean.AUTHORIZE_TIME_OUT_CODE, "刷新token超时");
            }
        }
        UserModel userModel = userService.checkUser(JwtUtil.getId(jwt));
        UserLoginDto userLoginDto = new UserLoginDto(userModel, JwtUtil.builder(userModel));
        return JsonMessage.getString(200, "", userLoginDto);
    }
}
