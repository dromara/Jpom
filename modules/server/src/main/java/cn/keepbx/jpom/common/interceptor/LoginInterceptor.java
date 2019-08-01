package cn.keepbx.jpom.common.interceptor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.URLEncoder;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.jpom.system.ExtConfigBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
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
        UserModel user = null;
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 记录请求类型
            boolean isPage = isPage(handlerMethod);
            request.setAttribute("Page_Req", isPage);
            //
            NotLogin notLogin = handlerMethod.getMethodAnnotation(NotLogin.class);
            if (notLogin == null) {
                user = (UserModel) session.getAttribute(SESSION_NAME);
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
        }
        reload();
        if (user != null) {
            checkRoleDesc(request, user);
        }
        //
        return true;
    }

    private void checkRoleDesc(HttpServletRequest request, UserModel userModel) {
        NodeModel node = (NodeModel) request.getAttribute("node");
        String roleDesc;
        if (userModel.isSystemUser()) {
            roleDesc = Role.System.getDesc();
        } else {
            if (node != null) {
                if (userModel.isManage(node.getId())) {
                    roleDesc = Role.NodeManage.getDesc();
                } else {
                    roleDesc = Role.User.getDesc();
                }
            } else if (userModel.isServerManager()) {
                roleDesc = Role.ServerManager.getDesc();
            } else {
                roleDesc = Role.User.getDesc();
            }
        }
        request.setAttribute("roleDesc", roleDesc);
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
            String url = getHeaderProxyPath(request) + "/login.html";
            String uri = request.getRequestURI();
            boolean hasPar = false;
            if (StrUtil.isNotEmpty(uri) && !StrUtil.SLASH.equals(uri)) {
                url += "?url=" + uri;
                hasPar = true;
            }
            String header = request.getHeader(HttpHeaders.REFERER);
            if (header != null) {
                if (hasPar) {
                    url += "&";
                } else {
                    url += "?";
                }
                url += "r=" + header;
            }
            response.sendRedirect(url);
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
            session.setAttribute("staticCacheTime", DateUtil.currentSeconds());
            // 代理二级路径
            Object jpomProxyPath = session.getAttribute("jpomProxyPath");
            if (jpomProxyPath == null) {
                String path = getHeaderProxyPath(request);
                session.setAttribute("jpomProxyPath", path);
            }
        } catch (Exception ignored) {
        }
        try {
            // 统一的js 注入
            String jsCommonContext = (String) session.getAttribute("jsCommonContext");
            if (jsCommonContext == null) {
                String path = ExtConfigBean.getInstance().getPath();
                File file = FileUtil.file(String.format("%s/script/common.js", path));
                if (file.exists()) {
                    jsCommonContext = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
                    jsCommonContext = URLEncoder.DEFAULT.encode(jsCommonContext, CharsetUtil.CHARSET_UTF_8);
                }
                session.setAttribute("jsCommonContext", jsCommonContext);
            }
        } catch (IllegalStateException ignored) {
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        super.afterCompletion(request, response, handler, ex);
        BaseServerController.remove();
    }


}
