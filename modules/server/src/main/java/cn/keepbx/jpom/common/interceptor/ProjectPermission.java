package cn.keepbx.jpom.common.interceptor;

import cn.keepbx.jpom.model.log.UserOperateLogV1;

import java.lang.annotation.*;

/**
 * 项目权限
 *
 * @author jiangzeyin
 * @date 2019/03/16
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

    /**
     * 验证上传权限
     *
     * @return bool
     */
    boolean checkUpload() default false;

    /**
     * 验证删除权限
     *
     * @return bool
     */
    boolean checkDelete() default false;

    /**
     * 操作类型
     *
     * @return 枚举
     */
    UserOperateLogV1.OptType optType();
}
