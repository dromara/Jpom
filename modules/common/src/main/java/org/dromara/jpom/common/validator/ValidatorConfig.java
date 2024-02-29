/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.validator;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * 字段验证配置
 *
 * @author bwcx_jzy
 * @since 2018/8/21.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidatorConfig {
    /**
     * 需要验证的规则
     *
     * @return ValidatorItem
     */
    ValidatorItem[] value() default
        {
            @ValidatorItem(value = ValidatorRule.NOT_EMPTY)
        };

    /**
     * 自动参数值
     *
     * @return url 参数
     */
    String name() default "";

    /**
     * 默认值
     *
     * @return 默认
     */
    String defaultVal() default ValueConstants.DEFAULT_NONE;

    /**
     * 自定义验证 Controller 中方法名
     * <p>
     * public boolean customizeValidator(MethodParameter methodParameter, String value)
     *
     * @return 默认 customizeValidator
     */
    String customizeMethod() default "customizeValidator";

    /**
     * 判断参数为空 是字符串空
     * 如果为false
     *
     * @return 默认true
     */
    boolean strEmpty() default true;

    /**
     * 错误条件
     * <p>
     * or  一项正确返回正确，所有错误抛出错误
     * <p>
     * and 一项错误 抛出错误并结束整个判断
     *
     * @return 默认or
     */
    ErrorCondition errorCondition() default ErrorCondition.AND;
}
