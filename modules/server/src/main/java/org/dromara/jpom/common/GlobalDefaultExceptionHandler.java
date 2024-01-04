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
package org.dromara.jpom.common;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.exception.AgentAuthorizeException;
import org.dromara.jpom.exception.AgentException;
import org.dromara.jpom.exception.BaseExceptionHandler;
import org.dromara.jpom.exception.PermissionException;
import org.dromara.jpom.transport.TransportAgentException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理
 *
 * @author bwcx_jzy
 * @since 2019/04/17
 */
@RestControllerAdvice
@Slf4j
public class GlobalDefaultExceptionHandler extends BaseExceptionHandler {

    /**
     * 声明要捕获的异常
     *
     * @param e 异常
     */
    @ExceptionHandler({AgentAuthorizeException.class})
    public IJsonMessage<String> delExceptionHandler(AgentAuthorizeException e) {
        return e.getJsonMessage();
    }

    /**
     * 插件端异常
     * <p>
     * 避免重复记录堆栈
     *
     * @param request 请求
     * @param e       异常
     * @author jzy
     * @since 2021-08-01
     */
    @ExceptionHandler({AgentException.class, TransportAgentException.class})
    public IJsonMessage<String> agentExceptionHandler(HttpServletRequest request, AgentException e) {
        Throwable cause = e.getCause();
        if (cause != null) {
            log.error("controller {}", request.getRequestURI(), cause);
        }
        return new JsonMessage<>(405, e.getMessage());
    }

    /**
     * 权限异常 需要退出登录
     *
     * @param e 异常
     * @return json
     */
    @ExceptionHandler({PermissionException.class})
    public IJsonMessage<String> doPermissionException(PermissionException e) {
        return new JsonMessage<>(ServerConst.AUTHORIZE_TIME_OUT_CODE, e.getMessage());
    }
}
