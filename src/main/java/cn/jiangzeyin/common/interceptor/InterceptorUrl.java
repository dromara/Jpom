package cn.jiangzeyin.common.interceptor;

import java.lang.annotation.*;

/**
 * Created by jiangzeyin on 2017/5/9.
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface InterceptorUrl {

    String[] value();
}
