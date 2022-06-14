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

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.system.JpomRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常处理
 *
 * @author jiangzeyin
 * @since 2019/04/17
 */
@ControllerAdvice
@Slf4j
public class AgentExceptionHandler extends BaseExceptionHandler {

    /**
     * 声明要捕获的异常
     *
     * @param request  请求
     * @param response 响应
     * @param e        异常
     */
    @ExceptionHandler({JpomRuntimeException.class, RuntimeException.class, Exception.class})
    public void defExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        log.error("controller " + request.getRequestURI(), e);
        if (e instanceof JpomRuntimeException) {
            ServletUtil.write(response, JsonMessage.getString(500, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        } else {
            ServletUtil.write(response, JsonMessage.getString(500, "服务异常：" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
        }
    }

    /**
     * 声明要捕获的异常 (参数或者状态异常)
     *
     * @param request  请求
     * @param response 响应
     * @param e        异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, ValidateException.class})
    public void paramExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
        log.error("controller " + request.getRequestURI(), e);
        ServletUtil.write(response, JsonMessage.getString(405, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
    }
}
