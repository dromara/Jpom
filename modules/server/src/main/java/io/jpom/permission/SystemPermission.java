package io.jpom.permission;

import java.lang.annotation.*;

/**
 * 系统管理的权限
 *
 * @author bwcx_jzy
 * @date 2019/8/17
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SystemPermission {
}
