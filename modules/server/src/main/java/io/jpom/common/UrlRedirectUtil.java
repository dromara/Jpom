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
package io.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Function;

/**
 * url 重定向
 * 配置nginx 代理实现
 *
 * @author bwcx_jzy
 * @since 2019/11/14
 */
public class UrlRedirectUtil {

//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request 请求
//	 * @param url     跳转url
//	 * @see javax.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static String getRedirect(HttpServletRequest request, String url) {
//		int port = getPort(request);
//		return getRedirect(request, url, port);
//	}

//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request 请求
//	 * @param url     跳转url
//	 * @see javax.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static String getRedirect(HttpServletRequest request, String url, int port) {
//		String proto = ServletUtil.getHeaderIgnoreCase(request, "X-Forwarded-Proto");
//		if (proto == null) {
//			return url;
//		} else {
//			String host = request.getHeader(HttpHeaders.HOST);
//			if (StrUtil.isEmpty(host)) {
//				throw new RuntimeException("请配置host header");
//			}
//			if ("http".equals(proto) && port == 0) {
//				port = 80;
//			} else if ("https".equals(proto) && port == 0) {
//				port = 443;
//			}
//			String format = StrUtil.format("{}://{}:{}{}", proto, host, port, url);
//			return URLUtil.normalize(format);
//		}
//	}

//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request  请求
//	 * @param response 响应
//	 * @param url      跳转url
//	 * @throws IOException io
//	 * @see javax.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url, int port) throws IOException {
//		String toUrl = getRedirect(request, url, port);
//		response.sendRedirect(toUrl);
//	}

//
//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request  请求
//	 * @param response 响应
//	 * @param url      跳转url
//	 * @throws IOException io
//	 * @see javax.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
//		int port = getPort(request);
//		sendRedirect(request, response, url, port);
//	}

	private static int getPort(HttpServletRequest request) {
		String proxyPort = ServletUtil.getHeaderIgnoreCase(request, "X-Forwarded-Port");
		int port = 0;
		if (StrUtil.isNotEmpty(proxyPort)) {
			port = Integer.parseInt(proxyPort);
		}
		return port;
	}

	/**
	 * 二级代理路径
	 *
	 * @param request req
	 * @return context-path+nginx配置
	 */
	public static String getHeaderProxyPath(HttpServletRequest request, String headName) {
		return getHeaderProxyPath(request, headName, null);
	}

	/**
	 * 二级代理路径
	 *
	 * @param request req
	 * @return context-path+nginx配置
	 */
	public static String getHeaderProxyPath(HttpServletRequest request, String headName, Function<String, String> function) {
		String proxyPath = ServletUtil.getHeaderIgnoreCase(request, headName);
		//
		if (StrUtil.isEmpty(proxyPath)) {
			return request.getContextPath();
		}
		// 回调处理
		if (function != null) {
			proxyPath = function.apply(proxyPath);
		}
		//
		proxyPath = FileUtil.normalize(request.getContextPath() + StrUtil.SLASH + proxyPath);
		if (proxyPath.endsWith(StrUtil.SLASH)) {
			proxyPath = proxyPath.substring(0, proxyPath.length() - 1);
		}
		return proxyPath;
	}
}
