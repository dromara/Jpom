package io.jpom.common.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import io.jpom.common.UrlRedirectUtil;
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

    public static final String PROXY_PATH = "Jpom-ProxyPath";

//    static boolean isPage(HandlerMethod handlerMethod) {
//        ResponseBody responseBody = handlerMethod.getMethodAnnotation(ResponseBody.class);
//        if (responseBody == null) {
//            RestController restController = handlerMethod.getBeanType().getAnnotation(RestController.class);
//            return restController == null;
//        }
//        return false;
//    }

    public static void sendRedirects(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        String newUrl = UrlRedirectUtil.getHeaderProxyPath(request, PROXY_PATH) + url;
        UrlRedirectUtil.sendRedirect(request, response, newUrl);
    }

    public static String getRedirect(HttpServletRequest request, String url) {
        String newUrl = UrlRedirectUtil.getHeaderProxyPath(request, PROXY_PATH) + url;
        String redirect = UrlRedirectUtil.getRedirect(request, newUrl);
        return String.format("redirect:%s", redirect);
    }

    public static String getHeaderProxyPath(HttpServletRequest request) {
        String proxyPath = ServletUtil.getHeaderIgnoreCase(request, PROXY_PATH);
        if (StrUtil.isEmpty(proxyPath)) {
            return StrUtil.EMPTY;
        }
        if (proxyPath.endsWith(StrUtil.SLASH)) {
            proxyPath = proxyPath.substring(0, proxyPath.length() - 1);
        }
        return proxyPath;
    }
}
