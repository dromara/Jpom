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
package io.jpom.system;

import ch.qos.logback.core.PropertyDefinerBase;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.interceptor.BaseCallbackController;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 自动记录日志
 *
 * @author jiangzeyin
 * @since 2017/5/11
 */
@Aspect
@Component
@Slf4j
public class WebAopLog extends PropertyDefinerBase {

	private static volatile AopLogInterface aopLogInterface;

	synchronized public static void setAopLogInterface(AopLogInterface aopLogInterface) {
		WebAopLog.aopLogInterface = aopLogInterface;
	}

	@Pointcut("execution(public * io.jpom.controller..*.*(..))")
	public void webLog() {
		//
	}

	@Around(value = "webLog()", argNames = "joinPoint")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		// 接收到请求，记录请求内容
		boolean consoleLogReqResponse = ExtConfigBean.getInstance().isConsoleLogReqResponse();
		Object proceed;
		Object logResult = null;
		try {
			if (aopLogInterface != null) {
				aopLogInterface.before(joinPoint);
			}
			proceed = joinPoint.proceed();
			logResult = proceed;
		} catch (Throwable e) {
			logResult = e;
			throw e;
		} finally {
			if (aopLogInterface != null) {
				aopLogInterface.afterReturning(logResult);
			}
		}
		if (consoleLogReqResponse && logResult != null) {
			log.info(BaseCallbackController.getRequestAttributes().getRequest().getRequestURI() + " :" + logResult);
		}
		return proceed;
	}

	@Override
	public String getPropertyValue() {
		String path = StringUtil.getArgsValue(JpomApplication.getArgs(), "jpom.log");
		if (StrUtil.isEmpty(path)) {
			//
			File file = JpomManifest.getRunPath();
			if (file.isFile()) {
				// jar 运行模式
				file = file.getParentFile().getParentFile();
			} else {
				// 本地调试模式 @author jzy 2021-08-02 程序运行时候不影响打包
				file = FileUtil.file(FileUtil.getParent(file, 2), "log");
				Console.log("log file save path ：" + FileUtil.getAbsolutePath(file));
			}
			path = FileUtil.getAbsolutePath(file);
		}
		// 配置默认日志路径
		//        DefaultSystemLog.configPath(path, false);
		return path;
	}
}
