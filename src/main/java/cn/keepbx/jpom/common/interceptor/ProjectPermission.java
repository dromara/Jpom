package cn.keepbx.jpom.common.interceptor;

import java.lang.annotation.*;

/**
 * 项目权限
 * Created by jiangzeyin on 2019/03/16.
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface ProjectPermission {
    /**
     * 项目id 参数
     *
     * @return 默认Id
     */
    String value() default "id";

    boolean checkUpload() default false;

    boolean checkDelete() default false;
}
