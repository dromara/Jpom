/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.controller.BaseMyErrorController;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.http.HttpStatus;
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

import javax.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;

/**
 * @author bwcx_jzy
 * @since 2022/4/16
 */
@Slf4j
public abstract class BaseExceptionHandler {

    /**
     * 声明要捕获的异常
     *
     * @param request 请求
     * @param e       异常
     */
    @ExceptionHandler({JpomRuntimeException.class, RuntimeException.class, Exception.class})
    @ResponseBody
    public IJsonMessage<String> defExceptionHandler(HttpServletRequest request, Exception e) {
        if (e instanceof JpomRuntimeException) {
            log.error("global handle exception: {} {}", request.getRequestURI(), e.getMessage(), e.getCause());
            return new JsonMessage<>(500, e.getMessage());
        } else {
            log.error("global handle exception: {}", request.getRequestURI(), e);
            boolean causedBy = ExceptionUtil.isCausedBy(e, AccessDeniedException.class);
            if (causedBy) {
                return new JsonMessage<>(500, I18nMessageUtil.get("i18n.operation_file_permission_exception.5a41") + e.getMessage());
            }
            return new JsonMessage<>(500, I18nMessageUtil.get("i18n.service_exception.3821") + e.getMessage());
        }
    }

    @ExceptionHandler({NullPointerException.class})
    @ResponseBody
    public IJsonMessage<String> defNullPointerExceptionHandler(HttpServletRequest request, Exception e) {
        log.error("global NullPointerException: {}", request.getRequestURI(), e);
        String jpomType = SystemUtil.get("JPOM_TYPE", StrUtil.EMPTY);
        return new JsonMessage<>(500, jpomType + I18nMessageUtil.get("i18n.program_error_null_pointer.12e1"));
    }

    /**
     * 声明要捕获的异常 (参数或者状态异常)
     *
     * @param request 请求
     * @param e       异常
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, ValidateException.class})
    @ResponseBody
    public IJsonMessage<String> paramExceptionHandler(HttpServletRequest request, Exception e) {
        if (log.isDebugEnabled()) {
            log.debug("controller  {}", request.getRequestURI(), e);
        } else {
            log.warn("controller {} {}", request.getRequestURI(), e.getMessage());
        }
        return new JsonMessage<>(405, e.getMessage());
    }


    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMessageConversionException.class})
    @ResponseBody
    public IJsonMessage<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn(I18nMessageUtil.get("i18n.parameter_parsing_exception.0056"), e.getMessage());
        return new JsonMessage<>(HttpStatus.EXPECTATION_FAILED.value(), I18nMessageUtil.get("i18n.incorrect_parameter_format.9efb"));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    @ResponseBody
    public IJsonMessage<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return new JsonMessage<>(HttpStatus.METHOD_NOT_ALLOWED.value(), I18nMessageUtil.get("i18n.unsupported_request_method.45d7"), e.getMessage());
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    @ResponseBody
    public IJsonMessage<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return new JsonMessage<>(HttpStatus.NOT_FOUND.value(), I18nMessageUtil.get("i18n.no_resource_found.dc22"), e.getMessage());
    }

    /**
     * 上传文件大小超出限制
     *
     * @param e 异常
     */
    @ExceptionHandler({MaxUploadSizeExceededException.class})
    @ResponseBody
    public IJsonMessage<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error(I18nMessageUtil.get("i18n.file_size_exceeds_limit.8272"), e);
        return new JsonMessage<>(HttpStatus.NOT_ACCEPTABLE.value(), BaseMyErrorController.FILE_MAX_SIZE_MSG.get(), e.getMessage());
    }

    @ExceptionHandler({ConstructorException.class})
    @ResponseBody
    public IJsonMessage<String> handleConstructorException(ConstructorException e) {
        log.warn(I18nMessageUtil.get("i18n.yml_configuration_content_error.08f8"), e);
        return new JsonMessage<>(HttpStatus.EXPECTATION_FAILED.value(), I18nMessageUtil.get("i18n.yml_config_format_error_illegal_field.16ea") + e.getMessage());
    }

    @ExceptionHandler({ScannerException.class})
    @ResponseBody
    public IJsonMessage<String> handleScannerException(ScannerException e) {
        log.warn("ScannerException", e);
        return new JsonMessage<>(HttpStatus.EXPECTATION_FAILED.value(), I18nMessageUtil.get("i18n.yml_config_format_error_tab.f629") + e.getMessage());
    }

}
