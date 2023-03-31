/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.common.validator;

import org.springframework.web.bind.annotation.ValueConstants;

import java.lang.annotation.*;

/**
 * 字段验证配置
 *
 * @author jiangzeyin
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
