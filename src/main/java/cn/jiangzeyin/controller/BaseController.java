package cn.jiangzeyin.controller;

import cn.jiangzeyin.common.interceptor.LoginInterceptor;
import cn.jiangzeyin.controller.base.AbstractBaseControl;

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
}
