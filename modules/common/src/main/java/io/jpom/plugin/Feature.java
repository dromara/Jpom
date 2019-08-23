package io.jpom.plugin;

import java.lang.annotation.*;

/**
 * 功能
 *
 * @author bwcx_jzy
 * @date 2019/8/13
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Feature {

    /**
     * 类
     *
     * @return ClassFeature
     */
    ClassFeature cls() default ClassFeature.NULL;

    /**
     * 方法
     *
     * @return MethodFeature
     */
    MethodFeature method() default MethodFeature.NULL;
}
