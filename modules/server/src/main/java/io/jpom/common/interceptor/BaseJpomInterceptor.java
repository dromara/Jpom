package io.jpom.common.interceptor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import org.springframework.http.HttpHeaders;
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
        url = getHeaderProxyPathAndContextPath(request) + url;
        int proxyPort = getHeaderProxyPort(request);

        sendRedirect(request, response, url, proxyPort);
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
            return 80;
        }
        if (StrUtil.contains(proxyPath, CharUtil.COLON)) {
            return Convert.toInt(StrUtil.split(proxyPath, StrUtil.COLON)[0], 80);
        }
        return 80;
    }


    /**
     * 二级代理路径
     *
     * @param request req
     * @return nginx配置 + context-path
     */
    private static String getHeaderProxyPathAndContextPath(HttpServletRequest request) {
        String proxyPath = getHeaderProxyPathNotPort(request);
        if (StrUtil.isEmpty(proxyPath)) {
            return request.getContextPath();
        }
        proxyPath = FileUtil.normalize(request.getContextPath() + StrUtil.SLASH + proxyPath);
        if (proxyPath.endsWith(StrUtil.SLASH)) {
            proxyPath = proxyPath.substring(0, proxyPath.length() - 1);
        }
        return proxyPath;
    }

    /**
     * 获取 protocol 协议完全跳转
     *
     * @param request  请求
     * @param response 响应
     * @param url      跳转url
     * @throws IOException io
     */
    public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url,
                                    int remotePortStr) throws IOException {
        String proto = ServletUtil.getHeaderIgnoreCase(request, "X-Forwarded-Proto");
        if (proto == null) {
            response.sendRedirect(url);
        } else {
            String host = request.getHeader(HttpHeaders.HOST);
            if (StrUtil.isEmpty(host)) {
                throw new RuntimeException("请配置host header");
            }
            String toUrl = StrUtil.format("{}://{}:{}{}", proto, host, remotePortStr, url);
            response.sendRedirect(toUrl);
        }
    }

}
