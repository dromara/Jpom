/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.system.AgentException;
import io.jpom.system.AuthorizeException;
import io.jpom.system.JpomRuntimeException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;

/**
 * 全局异常处理
 *
 * @author jiangzeyin
 * @date 2019/04/17
 */
@ControllerAdvice
public class GlobalDefaultExceptionHandler {

	/**
	 * 声明要捕获的异常
	 *
	 * @param request  请求
	 * @param response 响应
	 * @param e        异常
	 */
	@ExceptionHandler({AuthorizeException.class, RuntimeException.class, Exception.class})
	public void delExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
		DefaultSystemLog.getLog().error("global handle exception: {}", request.getRequestURI(), e);
		if (e instanceof AuthorizeException) {
			AuthorizeException authorizeException = (AuthorizeException) e;
			ServletUtil.write(response, authorizeException.getJsonMessage().toString(), MediaType.APPLICATION_JSON_VALUE);
		} else if (e instanceof JpomRuntimeException) {
			ServletUtil.write(response, JsonMessage.getString(500, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
		} else {
			boolean causedBy = ExceptionUtil.isCausedBy(e, AccessDeniedException.class);
			if (causedBy) {
				ServletUtil.write(response, JsonMessage.getString(500, "操作文件权限异常,请手动处理：" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
				return;
			}
			ServletUtil.write(response, JsonMessage.getString(500, "服务异常：" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
		}
	}

	/**
	 * 插件端异常
	 * <p>
	 * 避免重复记录堆栈
	 *
	 * @param request  请求
	 * @param response 响应
	 * @param e        异常
	 * @author jzy
	 * @since 2021-08-01
	 */
	@ExceptionHandler({AgentException.class})
	public void agentExceptionHandler(HttpServletRequest request, HttpServletResponse response, AgentException e) {
		Throwable cause = e.getCause();
		if (cause != null) {
			DefaultSystemLog.getLog().error("controller " + request.getRequestURI(), cause);
		}
		ServletUtil.write(response, JsonMessage.getString(405, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
	}

	/**
	 * git 仓库操作相关异常
	 *
	 * @param request  请求
	 * @param response 响应
	 * @param e        异常
	 */
	@ExceptionHandler({GitAPIException.class})
	public void gitExceptionHandler(HttpServletRequest request, HttpServletResponse response, GitAPIException e) {
		DefaultSystemLog.getLog().warn("GitAPIException: " + request.getRequestURI(), e);
		ServletUtil.write(response, JsonMessage.getString(405, "git 仓库操作异常:" + e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
	}

	/**
	 * 声明要捕获的异常 (参数，状态，验证异常)
	 *
	 * @param request  请求
	 * @param response 响应
	 * @param e        异常
	 */
	@ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, ValidateException.class})
	public void paramExceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception e) {
		DefaultSystemLog.getLog().error("controller " + request.getRequestURI(), e);
		ServletUtil.write(response, JsonMessage.getString(405, e.getMessage()), MediaType.APPLICATION_JSON_VALUE);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, HttpMessageConversionException.class})
	@ResponseBody
	public JsonMessage<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		DefaultSystemLog.getLog().warn("参数解析异常:{}", e.getMessage());
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
}
