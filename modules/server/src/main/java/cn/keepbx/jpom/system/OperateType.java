package cn.keepbx.jpom.system;

import cn.keepbx.jpom.model.data.UserOperateLogV1;

import java.lang.annotation.*;

/**
 * Created by jiangzeyin on 2019/4/19.
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateType {

    UserOperateLogV1.Type value();
}

