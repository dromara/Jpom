package io.jpom.common.interceptor;

import io.jpom.model.log.UserOperateLogV1;

import java.lang.annotation.*;

/**
 * 操作日志记录
 *
 * @author jiangzeyin
 * @date 2019/03/16
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface OptLog {

    /**
     * 操作类型
     *
     * @return 枚举
     */
    UserOperateLogV1.OptType value();
}
