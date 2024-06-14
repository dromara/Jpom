/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.db;

import java.lang.annotation.*;

/**
 * 数据库表名
 *
 * @author bwcx_jzy
 * @since 2021/8/13
 */
@Documented
@Target({ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface TableName {

    /**
     * 表名
     *
     * @return tableName
     */
    String value();

    /**
     * 表描述
     *
     * @return 描述
     */
    String nameKey();

    /**
     * 数据库默认
     *
     * @return 默认所有模式
     */
    DbExtConfig.Mode[] modes() default {};

    /**
     * 父级
     *
     * @return class
     */
    Class<?> parents() default Void.class;

    /**
     * 绑定关系
     * <p>
     * 1 严格模式，需要手动删除
     * 2 删除工作空间时自动删除
     * 3 父级数据为空时可以自动删除
     *
     * @return 数据绑定关系
     */
    int workspaceBind() default 1;
}
