/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.common.interceptor;

import cn.jiangzeyin.common.interceptor.BaseInterceptor;

/**
 * 拦截器
 *
 * @author jiangzeyin
 * @since 2019/4/16
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

//    public static void sendRedirects(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
//        String newUrl = UrlRedirectUtil.getHeaderProxyPath(request, PROXY_PATH) + url;
//        UrlRedirectUtil.sendRedirect(request, response, newUrl);
//    }
//
//    public static String getRedirect(HttpServletRequest request, String url) {
//        String newUrl = UrlRedirectUtil.getHeaderProxyPath(request, PROXY_PATH) + url;
//        String redirect = UrlRedirectUtil.getRedirect(request, newUrl);
//        return String.format("redirect:%s", redirect);
//    }

//    public static String getHeaderProxyPath(HttpServletRequest request) {
//        String proxyPath = ServletUtil.getHeaderIgnoreCase(request, PROXY_PATH);
//        if (StrUtil.isEmpty(proxyPath)) {
//            return StrUtil.EMPTY;
//        }
//        if (proxyPath.endsWith(StrUtil.SLASH)) {
//            proxyPath = proxyPath.substring(0, proxyPath.length() - 1);
//        }
//        return proxyPath;
//    }
}
