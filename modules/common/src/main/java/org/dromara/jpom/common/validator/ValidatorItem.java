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

import java.lang.annotation.*;

/**
 * 验证规则
 *
 * @author bwcx_jzy
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
    String msg() default "";

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
