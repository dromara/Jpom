package cn.keepbx.jpom.common.interceptor;

import java.lang.annotation.*;

/**
 * 游客可以访问的Controller 标记
 *
 * @author jiangzeyin
 * @date 2017/5/9.
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface NotLogin {

    /***
     *  是否为openapi 接口
     * @return 是将验证token 信息
     */
    boolean openApi() default false;
}
