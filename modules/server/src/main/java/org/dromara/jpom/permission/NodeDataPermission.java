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

import org.dromara.jpom.service.h2db.BaseNodeService;

import java.lang.annotation.*;

/**
 * @author bwcx_jzy
 * @since 2021/12/23
 */

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NodeDataPermission {

    /**
     * 参数名
     *
     * @return 默认ID
     */
    String parameterName() default "id";

    /**
     * 数据 class
     *
     * @return cls
     */
    Class<? extends BaseNodeService<?>> cls();
}
