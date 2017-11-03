package cn.jiangzeyin.common.interceptor;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 *
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/2/4.
 */
@InterceptorUrl({"/**"})
public class LoginInterceptor extends BaseInterceptor implements GetUserName {

    public static final String SESSION_NAME = "user";

    public LoginInterceptor() {
        put(this);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        super.preHandle(request, response, handler);
        String url = request.getRequestURI();
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
        // 系统管理限制
        if (url.startsWith("/sysadmin")) {
            if (!"admin".equals(user)) {
                response.sendRedirect(request.getContextPath() + "/login.html");
                return false;
            }
        }
        return true;
    }

    @Override
    public String getUserName() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        return (String) request.getSession().getAttribute(LoginInterceptor.SESSION_NAME);
    }

    public String getAllUserName() {
        return getUserName();
    }
}
