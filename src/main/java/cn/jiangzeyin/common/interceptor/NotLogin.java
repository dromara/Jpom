package cn.jiangzeyin.common.interceptor;

import java.lang.annotation.*;

/**
 * Created by jiangzeyin on 2017/5/9.
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface NotLogin {
}
