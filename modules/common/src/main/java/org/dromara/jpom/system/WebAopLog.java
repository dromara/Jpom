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
package org.dromara.jpom.system;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.Optional;

/**
 * 自动记录日志
 *
 * @author jiangzeyin
 * @since 2017/5/11
 */
@Aspect
@Component
@Slf4j
public class WebAopLog {

    private final Collection<AopLogInterface> aopLogInterface;

    public WebAopLog() {
        this.aopLogInterface = SpringUtil.getBeansOfType(AopLogInterface.class).values();
    }

    @Pointcut("execution(public * io.jpom..*.*.controller..*.*(..)) || execution(public * io.jpom.controller..*.*(..))")
    public void webLog() {
        //
    }

    @Around(value = "webLog()", argNames = "joinPoint")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 接收到请求，记录请求内容
        Object proceed;
        Object logResult = null;
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            // 可能其他方式执行
            return joinPoint.proceed();
        }
        String requestUri = requestAttributes.getRequest().getRequestURI();
        try {
            aopLogInterface.forEach(aopLogInterface -> aopLogInterface.before(joinPoint));
            proceed = joinPoint.proceed();
            logResult = proceed;
            log.debug("{} {}", requestUri, Optional.ofNullable(proceed).orElse(StrUtil.EMPTY));
        } catch (Throwable e) {
            // 不用记录异常日志，全局异常拦截里面会记录，此处不用重复记录
            // log.debug("发生异常 {}", requestUri, e);
            logResult = e;
            throw e;
        } finally {
            Object finalLogResult = logResult;
            aopLogInterface.forEach(aopLogInterface -> aopLogInterface.afterReturning(finalLogResult));
        }
        return proceed;
    }
}
