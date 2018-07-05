package cn.jiangzeyin.common.interceptor;

import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 *
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/2/4.
 */
@InterceptorPattens
public class LoginInterceptor extends BaseInterceptor {

    public static final String SESSION_NAME = "user";
    public static final String SESSION_PWD = "pwd";


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        super.preHandle(request, response, handler);
        //String url = request.getRequestURI();
        String user = (String) session.getAttribute(SESSION_NAME);
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            NotLogin notLogin = handlerMethod.getMethodAnnotation(NotLogin.class);
            if (notLogin == null) {
                if (user == null) {
                    response.sendRedirect(request.getContextPath() + "/login.html");
                    return false;
                }
            }
        }
        reload();
        return true;
    }
}
