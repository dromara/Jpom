package cn.keepbx.jpom.common;

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
    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public void resetInfo() {
        USER_MODEL_THREAD_LOCAL.set(getUserModel());
    }

    protected UserModel getUser() {
        return USER_MODEL_THREAD_LOCAL.get();
    }

    public static void remove() {
        USER_MODEL_THREAD_LOCAL.remove();
    }

    private static UserModel getUserModel() {
        return (UserModel) getRequestAttributes().getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }

    /**
     * 获取当前登录人
     *
     * @return 用户名
     */
    public static String getUserName() {
        UserModel userModel = getUserModel();
        if (userModel == null) {
            return null;
        }
        return userModel.getId();
    }
}
