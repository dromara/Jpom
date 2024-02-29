/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
 * @author bwcx_jzy
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

    @Pointcut("execution(public * org.dromara.jpom..*.*.controller..*.*(..)) || execution(public * org.dromara.jpom.controller..*.*(..))")
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
