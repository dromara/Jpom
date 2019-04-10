package cn.keepbx.jpom.common.interceptor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.model.UserModel;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器
 *
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/2/4.
 */
@InterceptorPattens()
public class LoginInterceptor extends BaseInterceptor {
    /**
     * session
     */
    public static final String SESSION_NAME = "user";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        super.preHandle(request, response, handler);
        HttpSession session = getSession();
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            NotLogin notLogin = handlerMethod.getMethodAnnotation(NotLogin.class);
            if (notLogin == null) {
                UserModel user = (UserModel) session.getAttribute(SESSION_NAME);
                if (user == null) {
                    response.sendRedirect(getHeaderProxyPath(request) + "/login.html");
                    return false;
                }
            }
        }
        reload();
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
        HttpSession session = getSession();
        try {
            Object staticCacheTime = session.getAttribute("staticCacheTime");
            if (staticCacheTime == null) {
                session.setAttribute("staticCacheTime", DateUtil.currentSeconds());
            }
            //
            Object jpomProxyPath = session.getAttribute("jpomProxyPath");
            if (jpomProxyPath == null) {
                String path = getHeaderProxyPath(request);
                session.setAttribute("jpomProxyPath", path);
            }
        } catch (Exception ignored) {
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
        BaseController.remove();
    }

    private String getHeaderProxyPath(HttpServletRequest request) {
        String proxyPath = ServletUtil.getHeader(request, "Jpom-ProxyPath", CharsetUtil.UTF_8);
        if (proxyPath == null) {
            proxyPath = ServletUtil.getHeader(request, "Jpom-ProxyPath".toLowerCase(), CharsetUtil.UTF_8);
        }
        if (StrUtil.isEmpty(proxyPath)) {
            return StrUtil.EMPTY;
        }
        proxyPath = FileUtil.normalize(proxyPath);
        if (proxyPath.endsWith(StrUtil.SLASH)) {
            proxyPath = proxyPath.substring(0, proxyPath.length() - 1);
        }
        return proxyPath;
    }
}
