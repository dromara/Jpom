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
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.Const;
import io.jpom.common.JsonMessage;
import io.jpom.system.AgentAuthorize;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 授权拦截
 *
 * @author jiangzeyin
 * @since 2019/4/17
 */
//@InterceptorPattens()
@Configuration
public class AuthorizeInterceptor implements HandlerMethodInterceptor {

    private final AgentAuthorize agentAuthorize;

    public AuthorizeInterceptor(AgentAuthorize agentAuthorize) {
        this.agentAuthorize = agentAuthorize;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        NotAuthorize notAuthorize = handlerMethod.getMethodAnnotation(NotAuthorize.class);
        if (notAuthorize == null) {
            String authorize = ServletUtil.getHeaderIgnoreCase(request, Const.JPOM_AGENT_AUTHORIZE);
            if (StrUtil.isEmpty(authorize)) {
                this.error(response);
                return false;
            }
            if (!agentAuthorize.checkAuthorize(authorize)) {
                this.error(response);
                return false;
            }
        }
        return true;
    }

    private void error(HttpServletResponse response) {
        ServletUtil.write(response, JsonMessage.getString(Const.AUTHORIZE_ERROR, "授权信息错误"), MediaType.APPLICATION_JSON_VALUE);
    }
}
