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
package top.jpom.storage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.util.Assert;
import top.jpom.db.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/1/5
 */
public class MysqlTableBuilderImpl implements IStorageSqlBuilderService {

    @Override
    public DbExtConfig.Mode mode() {
        return DbExtConfig.Mode.MYSQL;
    }

    @Override
    public String generateIndexSql(List<TableViewIndexData> row) {
        StringBuilder stringBuilder = new StringBuilder();
        for (TableViewIndexData viewIndexData : row) {
            String indexType = viewIndexData.getIndexType();
            switch (indexType) {
                case "ADD-UNIQUE":
                    // ALTER TABLE `jpom`.`PROJECT_INFO`
                    //DROP INDEX `workspaceId`,
                    //ADD UNIQUE INDEX `workspaceId`(`workspaceId` ASC, `strike` ASC, `modifyUser`) USING BTREE;
                    String field = viewIndexData.getField();
                    List<String> fields = StrUtil.splitTrim(field, "+");
                    Assert.notEmpty(fields, "索引未配置字段");
                    stringBuilder.append("call drop_index_if_exists('").append(viewIndexData.getTableName()).append("','").append(viewIndexData.getName()).append("')").append(";").append(StrUtil.LF);
                    stringBuilder.append(this.delimiter()).append(StrUtil.LF);
                    stringBuilder.append("ALTER TABLE ").append(viewIndexData.getTableName()).append(" ADD UNIQUE INDEX ").append(viewIndexData.getName()).append(" (").append(CollUtil.join(fields, StrUtil.COMMA)).append(")");
                    break;
                default:
                    throw new IllegalArgumentException("不支持的类型：" + indexType);
            }
            stringBuilder.append(";").append(StrUtil.LF);
            stringBuilder.append(this.delimiter()).append(StrUtil.LF);

        }
        return stringBuilder.toString();
    }

    @Override
    public String generateAlterTableSql(List<TableViewAlterData> row) {
        StringBuilder stringBuilder = new StringBuilder();
        for (TableViewAlterData viewAlterData : row) {
            String alterType = viewAlterData.getAlterType();
            switch (alterType) {
                case "DROP":
                    // ALTER TABLE NODE_INFO DROP COLUMN IF EXISTS `cycle`;
                    stringBuilder.append("CALL drop_column_if_exists('").append(viewAlterData.getTableName()).append("', '").append(viewAlterData.getName()).append("')");
                    break;
                case "ADD":
                    //  ALTER TABLE PROJECT_INFO ADD IF NOT EXISTS triggerToken VARCHAR (100) comment '触发器token';
                    String columnSql = this.generateColumnSql(viewAlterData, true);
                    stringBuilder.append("CALL add_column_if_not_exists('").append(viewAlterData.getTableName()).append("','").append(viewAlterData.getName()).append("','").append(columnSql).append("')");
                    break;
                case "ALTER":
                    // alter  table table1 modify  column column1  decimal(10,1) DEFAULT NULL COMMENT '注释';
                    stringBuilder.append("ALTER TABLE ").append(viewAlterData.getTableName()).append(" modify  column ");
                    stringBuilder.append(this.generateColumnSql(viewAlterData));
                    break;
                case "DROP-TABLE":
                    stringBuilder.append("drop table if exists ").append(viewAlterData.getTableName());
                    break;
                default:
                    throw new IllegalArgumentException("不支持的类型：" + alterType);
            }
            stringBuilder.append(";").append(StrUtil.LF);
            stringBuilder.append(this.delimiter()).append(StrUtil.LF);

        }
        return stringBuilder.toString();
    }

    /**
     * CREATE TABLE IF NOT EXISTS USEROPERATELOGV1
     * (
     * id        VARCHAR(50) not null comment 'id',
     * reqId     VARCHAR(50) COMMENT '请求ID',
     * CONSTRAINT USEROPERATELOGV1_PK PRIMARY KEY (id)
     * );
     * COMMENT ON TABLE USEROPERATELOGV1 is '操作日志';
     *
     * @param name 表名
     * @param desc 描述
     * @param row  字段信息
     * @return sql
     */
    @Override
    public String generateTableSql(String name, String desc, List<TableViewData> row) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CREATE TABLE IF NOT EXISTS ").append(name).append(StrUtil.LF);
        stringBuilder.append("(").append(StrUtil.LF);
        for (TableViewData tableViewData : row) {
            stringBuilder.append(StrUtil.TAB).append(this.generateColumnSql(tableViewData)).append(StrUtil.COMMA).append(StrUtil.LF);
        }
        // 主键
        List<String> primaryKeys = row.stream()
                .filter(tableViewData -> tableViewData.getPrimaryKey() != null && tableViewData.getPrimaryKey())
                .map(TableViewRowData::getName)
                .collect(Collectors.toList());
        Assert.notEmpty(primaryKeys, "表没有主键");
        stringBuilder.append(StrUtil.TAB).append("PRIMARY KEY (").append(CollUtil.join(primaryKeys, StrUtil.COMMA)).append(")").append(StrUtil.LF);
        stringBuilder.append(") ").append("COMMENT=").append("'").append(desc).append("';");
        return stringBuilder.toString();
    }

    @Override
    public String generateColumnSql(TableViewRowData tableViewRowData) {
        return generateColumnSql(tableViewRowData, false);
    }

    private String generateColumnSql(TableViewRowData tableViewRowData, boolean encode) {
        //        id VARCHAR(50) not null default '' comment 'id'
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("`").append(tableViewRowData.getName()).append("`").append(StrUtil.SPACE);
        String type = tableViewRowData.getType();
        Assert.hasText(type, "未正确配置数据类型");
        type = type.toUpperCase();
        switch (type) {
            case "LONG":
                stringBuilder.append("BIGINT").append(StrUtil.SPACE);
                break;
            case "STRING":
                stringBuilder.append("VARCHAR(").append(ObjectUtil.defaultIfNull(tableViewRowData.getLen(), 255)).append(")").append(StrUtil.SPACE);
                break;
            case "TEXT":
                stringBuilder.append("TEXT").append(StrUtil.SPACE);
                break;
            case "INTEGER":
                stringBuilder.append("int").append(StrUtil.SPACE);
                break;
            case "TINYINT":
                stringBuilder.append("TINYINT").append(StrUtil.SPACE);
                break;
            case "FLOAT":
                stringBuilder.append("float").append(StrUtil.SPACE);
                break;
            case "DOUBLE":
                stringBuilder.append("double").append(StrUtil.SPACE);
                break;
            default:
                throw new IllegalArgumentException("不支持的数据类型:" + type);
        }
        //
        Boolean notNull = tableViewRowData.getNotNull();
        if (notNull != null && notNull) {
            stringBuilder.append("not null").append(StrUtil.SPACE);
        }
        //
        String defaultValue = tableViewRowData.getDefaultValue();
        if (StrUtil.isNotEmpty(defaultValue)) {
            stringBuilder.append("default '").append(defaultValue).append("'").append(StrUtil.SPACE);
        }
        stringBuilder.append("comment '").append(tableViewRowData.getComment()).append("'");
        //
        String columnSql = stringBuilder.toString();
        if (encode) {
            columnSql = StrUtil.replace(columnSql, "'", "\\'");
        }
        int length = StrUtil.length(columnSql);
        Assert.state(length <= 180, "sql 语句太长啦");
        return columnSql;
    }

    @Override
    public String delimiter() {
        return "-- mysql delimiter";
    }
}
