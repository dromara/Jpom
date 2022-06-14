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
package io.jpom.controller;

import cn.jiangzeyin.common.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/3/17
 * @see org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
 */
@Slf4j
public abstract class BaseMyErrorController extends AbstractErrorController {

    public static final String FILE_MAX_SIZE_MSG = "上传文件太大了,请重新选择一个较小的文件上传吧";

    public BaseMyErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        log.error("发生异常：" + statusCode + "  " + requestUri);
        // 判断异常信息
        Object attribute = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Map<String, Object> body = new HashMap<>(5);
        body.put(JsonMessage.CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        String msg = "啊哦，好像哪里出错了，请稍候再试试吧~";
        if (attribute instanceof MaxUploadSizeExceededException) {
            // 上传文件大小异常
            msg = FILE_MAX_SIZE_MSG;
        } else if (status == HttpStatus.NOT_FOUND) {
            msg = "没有找到对应的资源";
            body.put(JsonMessage.DATA, requestUri);
        }
        body.put(JsonMessage.MSG, msg);

        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
