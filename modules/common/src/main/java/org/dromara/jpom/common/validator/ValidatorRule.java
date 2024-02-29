/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.validator;

/**
 * 验证规则
 *
 * @author bwcx_jzy
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
     * @see ParameterInterceptor#validatorNumber(cn.bwcx_jzy.common.validator.ValidatorItem, String)
     */
    DECIMAL,
    /**
     * 数字
     *
     * @see ParameterInterceptor#validatorNumber(cn.bwcx_jzy.common.validator.ValidatorItem, String)
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
