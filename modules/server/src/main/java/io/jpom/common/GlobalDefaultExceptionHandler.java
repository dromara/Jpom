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

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.system.AgentException;
import io.jpom.system.AuthorizeException;
import io.jpom.system.JpomRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;

/**
 * 全局异常处理
 *
 * @author jiangzeyin
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
    @ExceptionHandler({AuthorizeException.class})
    public JsonMessage<String> delExceptionHandler(AuthorizeException e) {
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
    @ExceptionHandler({AgentException.class})
    public JsonMessage<String> agentExceptionHandler(HttpServletRequest request, AgentException e) {
        Throwable cause = e.getCause();
        if (cause != null) {
            log.error("controller {}", request.getRequestURI(), cause);
        }
        return new JsonMessage<>(405, e.getMessage());
    }
}
