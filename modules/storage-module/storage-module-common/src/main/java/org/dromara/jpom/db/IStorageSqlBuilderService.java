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

import cn.hutool.core.util.StrUtil;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2023/1/5
 */
public interface IStorageSqlBuilderService extends IMode {

    /**
     * 生成表 sql
     *
     * @param name 表名称
     * @param desc 表描述
     * @param row  字段
     * @return sql
     */
    String generateTableSql(String name, String desc, List<TableViewData> row);

    /**
     * 生成 修改表 sql
     *
     * @param row 需要修改的字段
     * @return sql
     */
    String generateAlterTableSql(List<TableViewAlterData> row);

    /**
     * 生成 修改表 sql
     *
     * @param row 需要修改的字段
     * @return sql
     */
    String generateIndexSql(List<TableViewIndexData> row);

    /**
     * 根据字段信息生成 sql
     *
     * @param tableViewRowData 字段信息
     * @return sql
     */
    String generateColumnSql(TableViewRowData tableViewRowData);

    /**
     * sql 分隔执行标记
     *
     * @return 分隔标记
     */
    default String delimiter() {
        return StrUtil.EMPTY;
    }
}
