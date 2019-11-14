package io.jpom.common.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.CharUtil;
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

    static boolean isPage(HandlerMethod handlerMethod) {
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

    public static void sendRedirects(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        String newUrl = UrlRedirectUtil.getHeaderProxyPath(request, "Jpom-ProxyPath", s -> {
            if (StrUtil.contains(s, CharUtil.COLON)) {
                String[] split = StrUtil.split(s, StrUtil.COLON);
                return split[1];
            }
            return s;
        }) + url;
        int proxyPort = getHeaderProxyPort(request);
        UrlRedirectUtil.sendRedirect(request, response, newUrl, proxyPort);
    }

    private static String getHeaderProxyPath(HttpServletRequest request) {
        String proxyPath = ServletUtil.getHeaderIgnoreCase(request, "Jpom-ProxyPath");
        if (StrUtil.isEmpty(proxyPath)) {
            return StrUtil.EMPTY;
        }
        return proxyPath;
    }


    static String getHeaderProxyPathNotPort(HttpServletRequest request) {
        String proxyPath = getHeaderProxyPath(request);
        if (StrUtil.isEmpty(proxyPath)) {
            return StrUtil.EMPTY;
        }
        if (StrUtil.contains(proxyPath, CharUtil.COLON)) {
            String[] split = StrUtil.split(proxyPath, StrUtil.COLON);
            return split[1];
        }
        return proxyPath;
    }

    private static int getHeaderProxyPort(HttpServletRequest request) {
        String proxyPath = getHeaderProxyPath(request);
        if (StrUtil.isEmpty(proxyPath)) {
            return 0;
        }
        if (StrUtil.contains(proxyPath, CharUtil.COLON)) {
            String s = StrUtil.split(proxyPath, StrUtil.COLON)[0];
            return Integer.parseInt(s);
        }
        return 0;
    }
}
