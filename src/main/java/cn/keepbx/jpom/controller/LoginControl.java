package cn.keepbx.jpom.controller;

import cn.hutool.cache.impl.LFUCache;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.core.date.BetweenFormater;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.common.interceptor.NotLogin;
import cn.keepbx.jpom.model.UserModel;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.ExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录控制
 *
 * @author Administrator
 */
@Controller
public class LoginControl extends BaseController {

    private static final LFUCache<String, Integer> LFU_CACHE = new LFUCache<>(1000);

    private static final String LOGIN_CODE = "login_code";

    private static final String SHOW_CODE = "show_code";

    private static final int INPUT_CODE = 600;
    private static final int INPUT_CODE_ERROR_COUNT = 3;

    @Resource
    private UserService userService;

    /**
     * 登录页面
     *
     * @return login
     */
    @RequestMapping(value = "login.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public String login() {
        if (userService.userListEmpty()) {
            // 调整到初始化也
            return "redirect:install.html";
        }
        // 是否显示验证码
        setAttribute("showCode", showCode());
        return "login";
    }

    private boolean showCode() {
        String showCode = getSessionAttribute(SHOW_CODE);
        return StrUtil.isNotEmpty(showCode);
    }

    /**
     * 验证码
     *
     * @throws IOException IO
     */
    @RequestMapping(value = "randCode.png", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    @NotLogin
    public void randCode() throws IOException {
        CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(100, 50, 4, 8);
        circleCaptcha.createCode();
        HttpServletResponse response = getResponse();
        circleCaptcha.write(response.getOutputStream());
        String code = circleCaptcha.getCode();
        setSessionAttribute(LOGIN_CODE, code);
        // 回话显示验证码
        setSessionAttribute(SHOW_CODE, true);
    }

    private int ipError() {
        if (ExtConfigBean.getInstance().getIpErrorLockTime() <= 0) {
            return 0;
        }
        String ip = getIp();
        Integer count = LFU_CACHE.get(ip);
        if (count == null) {
            count = 0;
        }
        count++;
        LFU_CACHE.put(ip, count, ExtConfigBean.getInstance().getIpErrorLockTime());
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
        if (ExtConfigBean.getInstance().userAlwaysLoginError <= 0) {
            return false;
        }
        String ip = getIp();
        Integer count = LFU_CACHE.get(ip);
        if (count == null) {
            count = 0;
        }
        // 大于10倍时 封ip
        return count > ExtConfigBean.getInstance().userAlwaysLoginError * 10;
    }

    /**
     * 登录接口
     *
     * @param userName 登录名
     * @param userPwd  登录密码
     * @return json
     */
    @RequestMapping(value = "userLogin", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    @NotLogin
    public String userLogin(String userName, String userPwd, String code) throws IOException {
        if (StrUtil.isEmpty(userName) || StrUtil.isEmpty(userPwd)) {
            return JsonMessage.getString(405, "请输入登录信息");
        }
        if (this.ipLock()) {
            return JsonMessage.getString(400, "尝试次数太多，请稍后再来");
        }
        synchronized (UserModel.class) {
            UserModel userModel = userService.getItem(userName);
            if (userModel == null) {
                int error = this.ipError();
                return JsonMessage.getString(error >= INPUT_CODE_ERROR_COUNT ? INPUT_CODE : 400, "登录失败，请输入正确的密码和账号,多次失败将锁定账号");
            }
            if (showCode() || userModel.getPwdErrorCount() >= INPUT_CODE_ERROR_COUNT) {
                // 获取验证码
                String sCode = getSessionAttribute(LOGIN_CODE);
                if (StrUtil.isEmpty(code) || !sCode.equalsIgnoreCase(code)) {
                    return JsonMessage.getString(INPUT_CODE, "请输入正确的验证码");
                }
                removeSessionAttribute(LOGIN_CODE);
            }
            try {
                long lockTime = userModel.overLockTime();
                if (lockTime > 0) {
                    String msg = DateUtil.formatBetween(lockTime * 1000, BetweenFormater.Level.MINUTE);
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
                    return JsonMessage.getString(200, "登录成功");
                } else {
                    userModel.errorLock();
                    int rCode = 501;
                    if (userModel.getPwdErrorCount() > INPUT_CODE_ERROR_COUNT) {
                        // 启用验证码
                        rCode = INPUT_CODE;
                    }
                    this.ipError();
                    return JsonMessage.getString(rCode, "登录失败，请输入正确的密码和账号,多次失败将锁定账号");
                }
            } finally {
                userService.updateItem(userModel);
            }
        }
    }

    /**
     * 退出登录
     *
     * @return page
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String logout() {
        getSession().invalidate();
        return "redirect:index.html";
    }
}
