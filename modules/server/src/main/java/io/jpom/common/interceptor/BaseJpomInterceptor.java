package io.jpom.common.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 拦截器
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public abstract class BaseJpomInterceptor extends BaseInterceptor {

    public static boolean isPage(HandlerMethod handlerMethod) {
        ResponseBody responseBody = handlerMethod.getMethodAnnotation(ResponseBody.class);
        if (responseBody == null) {
            RestController restController = handlerMethod.getBeanType().getAnnotation(RestController.class);
            return restController == null;
        }
        return false;
    }

    public static boolean isPage(HttpServletRequest request) {
        return Convert.toBool(request.getAttribute("Page_Req"), true);
    }

    public void sendRedirects(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        url = getHeaderProxyPath(request) + url;
        BaseInterceptor.sendRedirect(request, response, url);
    }

    /**
     * 二级代理路径
     *
     * @param request req
     * @return nginx配置 + context-path
     */
    public static String getHeaderProxyPath(HttpServletRequest request) {
        String proxyPath = ServletUtil.getHeaderIgnoreCase(request, "Jpom-ProxyPath");
        if (StrUtil.isEmpty(proxyPath)) {
            return request.getContextPath();
        }
        proxyPath = FileUtil.normalize(request.getContextPath() + StrUtil.SLASH + proxyPath);
        if (proxyPath.endsWith(StrUtil.SLASH)) {
            proxyPath = proxyPath.substring(0, proxyPath.length() - 1);
        }
        return proxyPath;
    }


}
