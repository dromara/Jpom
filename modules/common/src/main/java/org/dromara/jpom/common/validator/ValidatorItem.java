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

import java.lang.annotation.*;

/**
 * 验证规则
 *
 * @author jiangzeyin
 * @since 2018/8/21
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ValidatorItem {
    /**
     * 规则
     *
     * @return ValidatorRule
     */
    ValidatorRule value() default ValidatorRule.NOT_EMPTY;

    /**
     * 还原html 转义字符
     * 一般用户字符串长度验证统一性
     *
     * @return 默认不还原
     */
    boolean unescape() default false;

    /**
     * 响应码
     *
     * @return 默认400
     */
    int code() default 400;

    /**
     * 错误信息
     *
     * @return msg
     */
    String msg() default "输入参数不正确";

    /**
     * 数字类型的范围
     * 配置错误将不判断
     * <p>
     * 范围写反也将不判断
     * <p>
     * 逻辑判断符 是 &gt; 或者 &lt;
     * <p>
     * 当 ValidatorRule 为 CUSTOMIZE 时此参数无效
     * <p>
     * 1 则为长度必须为1
     * <p>
     * 1.2:2  double类型的范围，值为1.2~2
     *
     * <p>
     * 1.2:2.5[1] double类型的范围，值为1.2~2.5 且小数点只能有一位
     *
     * @return 具体的规则
     * @see ValidatorRule#DECIMAL
     * @see ValidatorRule#NUMBERS
     * @see ValidatorRule#POSITIVE_INTEGER
     * @see ValidatorRule#NON_ZERO_INTEGERS
     * @see ValidatorRule#NOT_EMPTY
     * @see ValidatorRule#NOT_BLANK
     * @see ValidatorRule#GENERAL
     */
    String range() default "";
}
