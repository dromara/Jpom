/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.permission;

import java.lang.annotation.*;

/**
 * 功能
 *
 * @author bwcx_jzy
 * @since 2019/8/13
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Feature {

    /**
     * 类
     *
     * @return ClassFeature
     */
    ClassFeature cls() default ClassFeature.NULL;

    /**
     * 方法
     *
     * @return MethodFeature
     */
    MethodFeature method() default MethodFeature.NULL;

    /**
     * 是否记录响应 日志
     *
     * @return 默认 记录
     */
    boolean logResponse() default true;

    /**
     * 是否记录操作日志
     *
     * @return false 不记录操作日志
     */
    boolean log() default true;
}
