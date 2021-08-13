package io.jpom.common.interceptor;

import java.lang.annotation.*;

/**
 * 不需要授权
 *
 * @author jiangzeyin
 * @date 2019/4/17
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface NotAuthorize {
}

