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
}
