package io.jpom.system;

import org.aspectj.lang.JoinPoint;

/**
 * 日志接口
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public interface AopLogInterface {
    /**
     * 进入前
     *
     * @param joinPoint point
     */
    void before(JoinPoint joinPoint);

    /**
     * 执行后
     *
     * @param value 结果
     */
    void afterReturning(Object value);
}
