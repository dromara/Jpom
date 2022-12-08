package io.jpom.common;

import java.lang.annotation.*;

/**
 * @author bwcx_jzy
 * @since 2022/12/8
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface JpomAppType {

    Type value();
}
