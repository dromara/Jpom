package cn.keepbx.jpom.common.interceptor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseController;
import cn.keepbx.jpom.common.BaseNodeController;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 登录拦截器
 *
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/2/4.
 */
@InterceptorPattens()
public class LoginInterceptor extends BaseJpomInterceptor {
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
                    this.responseLogin(request, response, handlerMethod);
                    return false;
                }
                // 用户信息
                UserService userService = SpringUtil.getBean(UserService.class);
                UserModel newUser = userService.getItem(user.getId());
                if (newUser == null) {
                    // 用户被删除
                    this.responseLogin(request, response, handlerMethod);
                    return false;
                }
                if (user.getModifyTime() != newUser.getModifyTime()) {
                    // 被修改过
                    this.responseLogin(request, response, handlerMethod);
                    return false;
                }
            }

            //
            if (isPage(handlerMethod)) {
                if (BaseNodeController.class.isAssignableFrom(handlerMethod.getBeanType())) {
                    // 添加node
                }
            }
        }
        reload();
        //

        return true;
    }

    /**
     * 提示登录
     *
     * @param request       req
     * @param response      res
     * @param handlerMethod 方法
     * @throws IOException 异常
     */
    private void responseLogin(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws IOException {
        if (isPage(handlerMethod)) {
            response.sendRedirect(getHeaderProxyPath(request) + "/login.html");
            return;
        }
        ServletUtil.write(response, JsonMessage.getString(800, "登录信息已失效,重新登录"), MediaType.APPLICATION_JSON_UTF8_VALUE);
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        super.postHandle(request, response, handler, modelAndView);
        HttpSession session = getSession();
        try {
            // 静态资源地址参数
            Object staticCacheTime = session.getAttribute("staticCacheTime");
            if (staticCacheTime == null) {
                session.setAttribute("staticCacheTime", DateUtil.currentSeconds());
            }
            // 代理二级路径
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
        String proxyPath = ServletUtil.getHeaderIgnoreCase(request, "Jpom-ProxyPath");
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
