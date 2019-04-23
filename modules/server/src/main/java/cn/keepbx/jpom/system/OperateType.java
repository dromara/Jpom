package cn.keepbx.jpom.system;

import cn.keepbx.jpom.model.data.UserOperateLogV1;

import java.lang.annotation.*;

/**
 * 需要记录操作日志的
 * Created by jiangzeyin on 2019/4/19.
 */
@Documented
@Target(ElementType.METHOD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateType {

    UserOperateLogV1.OptType value();
}

