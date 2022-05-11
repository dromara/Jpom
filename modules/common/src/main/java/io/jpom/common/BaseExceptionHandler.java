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

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.controller.BaseMyErrorController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.scanner.ScannerException;

import javax.servlet.http.HttpServletResponse;

/**
 * @author bwcx_jzy
 * @since 2022/4/16
 */
@Slf4j
public abstract class BaseExceptionHandler {


    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMessageConversionException.class})
    @ResponseBody
    public JsonMessage<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("参数解析异常:{}", e.getMessage());
        return new JsonMessage<>(HttpStatus.EXPECTATION_FAILED.value(), "传入的参数格式不正确");
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    @ResponseBody
    public JsonMessage<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return new JsonMessage<>(HttpStatus.METHOD_NOT_ALLOWED.value(), "不被支持的请求方式", e.getMessage());
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    public void handleNoHandlerFoundException(HttpServletResponse response, NoHandlerFoundException e) {
        ServletUtil.write(response, JsonMessage.getString(HttpStatus.NOT_FOUND.value(), "没有找到对应的资源", e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 上传文件大小超出限制
     *
     * @param response 响应
     * @param e        异常
     */
    @ExceptionHandler({MaxUploadSizeExceededException.class})
    public void handleMaxUploadSizeExceededException(HttpServletResponse response, MaxUploadSizeExceededException e) {
        log.warn(e.getMessage());
        ServletUtil.write(response, JsonMessage.getString(HttpStatus.NOT_ACCEPTABLE.value(), BaseMyErrorController.FILE_MAX_SIZE_MSG, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
    }

    @ExceptionHandler({ConstructorException.class})
    public void handleConstructorException(HttpServletResponse response, ConstructorException e) {
        log.warn(e.getMessage());
        ServletUtil.write(response, JsonMessage.getString(HttpStatus.EXPECTATION_FAILED.value(), "yml 配置内容格式有误请检查后重新操作（请检查是否有非法字段）：" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
    }

    @ExceptionHandler({ScannerException.class})
    public void handleScannerException(HttpServletResponse response, ScannerException e) {
        log.warn(e.getMessage());
        ServletUtil.write(response, JsonMessage.getString(HttpStatus.EXPECTATION_FAILED.value(), "yml 配置内容格式有误请检查后重新操作（不要使用 \\t(TAB) 缩进）：" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
    }

}
