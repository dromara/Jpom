package cn.jiangzeyin.controller;

import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.controller.base.AbstractBaseControl;
import org.springframework.web.context.request.RequestAttributes;

/**
 * @author jiangzeyin
 * @date 2018/9/28
 */
public abstract class BaseController extends AbstractBaseControl {
    protected String userName;
    protected String userPwd;

    @Override
    public void resetInfo() {
        userName = getSessionAttribute(LoginInterceptor.SESSION_NAME);
        userPwd = getSessionAttribute(LoginInterceptor.SESSION_PWD);
    }

    public static String getUserName() {
        return (String) getRequestAttributes().getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }

    protected String getSocketPwd() {
        return SecureUtil.md5(String.format("%s:%s", userName, userPwd));
    }
}
