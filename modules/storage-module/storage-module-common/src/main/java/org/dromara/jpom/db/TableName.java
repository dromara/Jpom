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
    String name();

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
