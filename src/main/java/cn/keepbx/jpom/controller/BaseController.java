package cn.keepbx.jpom.controller;

import cn.jiangzeyin.controller.base.AbstractController;
import cn.keepbx.jpom.common.interceptor.LoginInterceptor;
import cn.keepbx.jpom.model.UserModel;
import org.springframework.web.context.request.RequestAttributes;

/**
 * base
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
public abstract class BaseController extends AbstractController {
    protected UserModel userName;

    @Override
    public void resetInfo() {
        userName = (UserModel) getSessionAttributeObj(LoginInterceptor.SESSION_NAME);
    }

    /**
     * 获取当前登录人
     *
     * @return 用户名
     */
    public static String getUserName() {
        UserModel userModel = (UserModel) getRequestAttributes().getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
        if (userModel == null) {
            return null;
        }
        return userModel.getId();
    }

//    protected String getSocketPwd() {
//        return userName.getUserMd5Key();
//    }
}
