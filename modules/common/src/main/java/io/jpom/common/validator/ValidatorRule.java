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
package io.jpom.common.validator;

/**
 * 验证规则
 *
 * @author jiangzeyin
 * @since  2018/8/21.
 */
public enum ValidatorRule {
    /**
     * 不为空
     */
    NOT_EMPTY,
    /**
     * 空
     */
    EMPTY,
    /**
     * 不为空白
     */
    NOT_BLANK,
    /**
     * 手机号码
     */
    MOBILE,
    /**
     * 邮箱
     */
    EMAIL,
    /**
     * 英文字母 、数字和下划线
     */
    GENERAL,
    /**
     * url
     */
    URL,
    /**
     * 汉字
     */
    CHINESE,
    /**
     * 是否是字母（包括大写和小写字母）
     */
    WORD,
    /**
     * 小数
     *
     * @see ParameterInterceptor#validatorNumber(cn.jiangzeyin.common.validator.ValidatorItem, String)
     */
    DECIMAL,
    /**
     * 数字
     *
     * @see ParameterInterceptor#validatorNumber(cn.jiangzeyin.common.validator.ValidatorItem, String)
     */
    NUMBERS,
    /**
     * 非零 正整数  最大长度7位
     */
    NON_ZERO_INTEGERS,
    /**
     * 正整数  包括0  最大长度7位
     */
    POSITIVE_INTEGER,
    /**
     * 自定义验证
     */
    CUSTOMIZE
}
