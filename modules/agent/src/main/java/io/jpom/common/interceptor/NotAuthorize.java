package io.jpom.common.interceptor;

import java.lang.annotation.*;

/**
 * 不需要授权
 * Created by jiangzeyin on 2019/4/17.
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface NotAuthorize {
}

