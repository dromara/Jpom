package cn.keepbx.jpom.common.interceptor;

import java.lang.annotation.*;

/**
 * 游客可以访问的Controller 标记
 * Created by jiangzeyin on 2017/5/9.
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface NotLogin {
}
