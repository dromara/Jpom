package cn.jiangzeyin.controller;

import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.controller.base.AbstractController;
import org.springframework.web.context.request.RequestAttributes;

/**
 * @author jiangzeyin
 * @date 2018/9/28
 */
public abstract class BaseController extends AbstractController {
    protected String userName;
    private String userPwd;

    @Override
    public void resetInfo() {
        userName = getSessionAttribute(LoginInterceptor.SESSION_NAME);
        userPwd = getSessionAttribute(LoginInterceptor.SESSION_PWD);
    }

    /**
     * 获取当前登录人
     *
     * @return 用户名
     */
    public static String getUserName() {
        return (String) getRequestAttributes().getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }

    protected String getSocketPwd() {
        return SecureUtil.md5(String.format("%s:%s", userName, userPwd));
    }
}
