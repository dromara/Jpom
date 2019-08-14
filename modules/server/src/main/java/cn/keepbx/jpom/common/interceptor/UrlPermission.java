package cn.keepbx.jpom.common.interceptor;

import cn.keepbx.jpom.model.Role;
import cn.keepbx.jpom.model.log.UserOperateLogV1;

import java.lang.annotation.*;

/**
 * url权限
 *
 * @author jiangzeyin
 * @date 2019/04/13
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlPermission {
    /**
     * 用户角色
     *
     * @return 普通用户
     */
    Role value() default Role.User;

    /**
     * 操作类型
     *
     * @return 枚举
     */
    UserOperateLogV1.OptType optType();
}
