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

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.interceptor.BaseInterceptor;
import cn.jiangzeyin.common.interceptor.InterceptorPattens;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author bwcx_jzy
 * @since 2019/9/4
 */
@InterceptorPattens(value = "/api/**")
public class OpenApiInterceptor extends BaseInterceptor {

	@Override
	protected boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

		NotLogin methodAnnotation = handlerMethod.getMethodAnnotation(NotLogin.class);
		if (methodAnnotation == null) {
			if (handlerMethod.getBeanType().isAnnotationPresent(NotLogin.class)) {
				return true;
			}
		} else {
			return true;
		}
		String checkOpenApi = this.checkOpenApi(request);
		if (checkOpenApi == null) {
			return true;
		}
		ServletUtil.write(response, checkOpenApi, MediaType.APPLICATION_JSON_VALUE);
		return false;
	}

	private String checkOpenApi(HttpServletRequest request) {
		String header = request.getHeader(ServerOpenApi.HEAD);
		if (StrUtil.isEmpty(header)) {
			return JsonMessage.getString(300, "token empty");
		}
		String authorizeToken = ServerExtConfigBean.getInstance().getAuthorizeToken();
		if (StrUtil.isEmpty(authorizeToken)) {
			return JsonMessage.getString(300, "not config token");
		}
		String md5 = SecureUtil.md5(authorizeToken);
		md5 = SecureUtil.sha1(md5 + ServerOpenApi.HEAD);
		if (!StrUtil.equals(header, md5)) {
			return JsonMessage.getString(300, "not config token");
		}
		return null;
	}
}
