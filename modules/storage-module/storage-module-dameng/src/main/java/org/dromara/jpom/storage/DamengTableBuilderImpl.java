/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.storage;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.*;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2025/5/8
 */
public class DamengTableBuilderImpl implements IStorageSqlBuilderService {

    @Override
    public DbExtConfig.Mode mode() {
        return DbExtConfig.Mode.DAMENG;
    }

    /**
     * 为标识符添加引号（达梦数据库使用双引号）。
     * @param identifier 标识符字符串
     * @return 加引号后的标识符，如果原标识符为空则返回原样
     */
    private String quoteIdentifier(String identifier) {
        if (StrUtil.isEmpty(identifier)) {
            return identifier;
        }
        // 达梦数据库使用双引号来界定标识符，以保留大小写或允许特殊字符
        return "\"" + identifier + "\"";
    }

    /**
     * 为一组标识符添加引号。
     * @param identifiers 标识符列表
     * @return 加引号后的标识符列表
     */
    private List<String> quoteIdentifiers(List<String> identifiers) {
        if (CollUtil.isEmpty(identifiers)) {
            return identifiers;
        }
        return identifiers.stream().map(this::quoteIdentifier).collect(Collectors.toList());
    }

    @Override
    public String generateIndexSql(List<TableViewIndexData> row) {
        StringBuilder stringBuilder = new StringBuilder();
        for (TableViewIndexData viewIndexData : row) {
            String indexType = viewIndexData.getIndexType();
            // 用于 CREATE INDEX 语句的表名和索引名需要加引号
            String tableNameForCreateIndex = quoteIdentifier(viewIndexData.getTableName());
            String indexNameForCreateIndex = quoteIdentifier(viewIndexData.getName());

            String field = viewIndexData.getField();
            List<String> fields = StrUtil.splitTrim(field, "+");
            Assert.notEmpty(fields, I18nMessageUtil.get("i18n.index_field_not_configured.96d9"));
            String columnsToIdx = CollUtil.join(quoteIdentifiers(fields), StrUtil.COMMA);

            // 调用存储过程。假设存储过程内部处理未加引号的名称，或根据数据库设置处理大小写。
            // 如果存储过程期望接收已加引号的名称，则应在此处传递 tableNameForCreateIndex 和 indexNameForCreateIndex。
            // 通常，传递给存储过程的字符串参数是原始名称。
            stringBuilder.append("CALL drop_index_if_exists('").append(viewIndexData.getTableName()).append("','").append(viewIndexData.getName()).append("')").append(";").append(StrUtil.LF);
            stringBuilder.append(this.delimiter()).append(StrUtil.LF);

            switch (indexType) {
                case "ADD-UNIQUE":
                    // 达梦: CREATE UNIQUE INDEX "索引名" ON "表名" ("列1", "列2", ...);
                    stringBuilder.append("CREATE UNIQUE INDEX ").append(indexNameForCreateIndex).append(" ON ").append(tableNameForCreateIndex).append(" (").append(columnsToIdx).append(")");
                    break;
                case "ADD":
                    // 达梦: CREATE INDEX "索引名" ON "表名" ("列1", "列2", ...);
                    stringBuilder.append("CREATE INDEX ").append(indexNameForCreateIndex).append(" ON ").append(tableNameForCreateIndex).append(" (").append(columnsToIdx).append(")");
                    break;
                default:
                    throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_type_with_colon2.7de2") + indexType);
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
            // DDL语句中的表名需要加引号
            String tableNameForDdl = quoteIdentifier(viewAlterData.getTableName());
            // 从 viewAlterData 获取的原始列名 (未加引号)
            String columnNameUnquoted = viewAlterData.getName();

            switch (alterType) {
                case "DROP":
                    // 调用存储过程，传递原始未加引号的名称
                    stringBuilder.append("CALL drop_column_if_exists('").append(viewAlterData.getTableName()).append("', '").append(columnNameUnquoted).append("')");
                    break;
                case "ADD":
                    // generateColumnSql 方法会处理其自身列名的引号。
                    // encode=true 用于在字符串字面量内转义, includeComment=true
                    String columnSqlForAdd = this.generateColumnSql(viewAlterData, true, true);
                    // 调用存储过程，传递原始未加引号的表名/列名
                    stringBuilder.append("CALL add_column_if_not_exists('").append(viewAlterData.getTableName()).append("','").append(columnNameUnquoted).append("','").append(columnSqlForAdd).append("')");
                    break;
                case "ALTER":
                    // 达梦: ALTER TABLE "表名" MODIFY ("列定义不含注释");
                    // 列注释的更新在达梦中不通过 MODIFY 内联处理。
                    // 调用 generateColumnSql 时设置 includeComment=false
                    String columnSqlForModify = this.generateColumnSql(viewAlterData, false, false);
                    stringBuilder.append("ALTER TABLE ").append(tableNameForDdl).append(" MODIFY (").append(columnSqlForModify).append(")");
                    // 如果需要更新注释，需要一个独立的 "COMMENT ON COLUMN" 语句。
                    // 当前框架可能不支持为一个 "ALTER" 操作生成两条语句。
                    // 因此，通过此构建器的 ALTER MODIFY 操作实际上会跳过注释的更新。
                    break;
                case "DROP-TABLE":
                    stringBuilder.append("DROP TABLE IF EXISTS ").append(tableNameForDdl);
                    break;
                default:
                    throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_type_with_colon2.7de2") + alterType);
            }
            stringBuilder.append(";").append(StrUtil.LF); // SQL语句结束符
            stringBuilder.append(this.delimiter()).append(StrUtil.LF); // 追加自定义分隔符
        }
        return stringBuilder.toString();
    }

    /**
     * 生成创建表的SQL语句。
     * 如果提供了表描述(desc)，则会额外生成一条 COMMENT ON TABLE 语句。
     * @param name 表名
     * @param desc 表描述
     * @param row  列定义列表
     * @return 生成的SQL字符串，可能包含多条语句（CREATE TABLE 和 COMMENT ON TABLE）
     */
    @Override
    public String generateTableSql(String name, String desc, List<TableViewData> row) {
        StringBuilder stringBuilder = new StringBuilder();
        String tableNameQuoted = quoteIdentifier(name); // 表名加引号
        stringBuilder.append("CREATE TABLE IF NOT EXISTS ").append(tableNameQuoted).append(StrUtil.LF);
        stringBuilder.append("(").append(StrUtil.LF);

        boolean hasColumns = false;
        if (CollUtil.isNotEmpty(row)) {
            List<String> columnSqlList = row.stream()
                .map(tableViewData -> StrUtil.TAB + this.generateColumnSql(tableViewData, false, true)) // encode=false, includeComment=true (列内联注释)
                .collect(Collectors.toList());

            if (!columnSqlList.isEmpty()) {
                stringBuilder.append(String.join("," + StrUtil.LF, columnSqlList));
                hasColumns = true;
            }
        }

        List<String> primaryKeyNames = row.stream()
            .filter(tableViewData -> tableViewData.getPrimaryKey() != null && tableViewData.getPrimaryKey())
            .map(TableViewRowData::getName)
            .filter(StrUtil::isNotEmpty)
            .collect(Collectors.toList());

        if (CollUtil.isNotEmpty(primaryKeyNames)) {
            if (hasColumns) { // 如果前面有列定义，则在主键前加逗号和换行
                stringBuilder.append(",").append(StrUtil.LF);
            } else { // 如果没有其他列，主键是第一项，确保换行
                stringBuilder.append(StrUtil.LF);
            }
            stringBuilder.append(StrUtil.TAB).append("PRIMARY KEY (").append(CollUtil.join(quoteIdentifiers(primaryKeyNames), StrUtil.COMMA)).append(")");
        }

        stringBuilder.append(StrUtil.LF).append(")"); // 结束表定义括号
        stringBuilder.append(this.delimiter()); // CREATE TABLE 语句结束

        // 如果有表描述，则生成独立的 COMMENT ON TABLE 语句
        if (StrUtil.isNotEmpty(desc)) {
            stringBuilder.append(StrUtil.LF); // 换行开始新的语句
            // 注意：这里不主动添加 this.delimiter()，因为这个方法返回的是一个“逻辑块”的SQL。
            // 如果调用者（如InitDb）期望在多个逻辑块之间添加分隔符，它会在循环外添加。
            // 如果InitDb把这个返回的字符串直接执行，分号已经足够分隔语句了。
            stringBuilder.append("COMMENT ON TABLE ").append(tableNameQuoted).append(" IS '").append(StrUtil.replace(desc, "'", "''")).append("';"); // 转义描述中的单引号
        }
        return stringBuilder.toString();
    }

    @Override
    public String generateColumnSql(TableViewRowData tableViewRowData) {
        // 默认调用: encode=false (不为存储过程参数转义), includeComment=true (包含列注释)
        return generateColumnSql(tableViewRowData, false, true);
    }

    /**
     * 生成单个列定义的SQL片段。
     * @param tableViewRowData 列的元数据
     * @param encode 是否为用作SQL字符串字面量内部值而转义特殊字符 (例如，传递给存储过程的参数)
     * @param includeComment 是否在列定义中包含 COMMENT 子句 (用于 CREATE TABLE)
     * @return 生成的列定义SQL字符串
     */
    private String generateColumnSql(TableViewRowData tableViewRowData, boolean encode, boolean includeComment) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(quoteIdentifier(tableViewRowData.getName())).append(StrUtil.SPACE); // 列名加引号

        String type = tableViewRowData.getType();
        Assert.hasText(type, I18nMessageUtil.get("i18n.data_type_not_configured_correctly.bf16"));
        String upperType = type.toUpperCase(); // 使用本地变量存储大写类型

        // 数据类型映射
        switch (upperType) {
            case "LONG":
                stringBuilder.append("BIGINT").append(StrUtil.SPACE);
                break;
            case "STRING":
                stringBuilder.append("VARCHAR(").append(ObjectUtil.defaultIfNull(tableViewRowData.getLen(), 255)).append(")").append(StrUtil.SPACE);
                break;
            case "TEXT":
                stringBuilder.append("CLOB").append(StrUtil.SPACE); // 达梦使用 CLOB 存储大文本
                break;
            case "INTEGER":
                stringBuilder.append("INT").append(StrUtil.SPACE);
                break;
            case "TINYINT":
                stringBuilder.append("TINYINT").append(StrUtil.SPACE); // 达梦 TINYINT 是有符号的 (-128 到 127)
                break;
            case "FLOAT":
                stringBuilder.append("REAL").append(StrUtil.SPACE); // 达梦 REAL 对应单精度浮点数
                break;
            case "DOUBLE":
                stringBuilder.append("DOUBLE PRECISION").append(StrUtil.SPACE); // 达梦 DOUBLE PRECISION 对应双精度浮点数
                break;
            default:
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.data_type_not_supported.fd03") + type);
        }

        // 处理 NOT NULL 约束
        Boolean notNull = tableViewRowData.getNotNull();
        if (notNull != null && notNull) {
            stringBuilder.append("NOT NULL").append(StrUtil.SPACE);
        } else {
            // 显式添加 NULL，更清晰，尽管通常省略 NOT NULL 时默认为 NULL
            stringBuilder.append("NULL").append(StrUtil.SPACE);
        }

        // 处理默认值
        String defaultValue = tableViewRowData.getDefaultValue();
        if (StrUtil.isNotEmpty(defaultValue)) {
            stringBuilder.append("DEFAULT ");
            // 仅对字符串/文本类类型的默认值加引号
            if (upperType.equals("STRING") || upperType.equals("TEXT")) { // TEXT 已映射为 CLOB
                stringBuilder.append("'").append(StrUtil.replace(defaultValue, "'", "''")).append("'"); // 达梦转义单引号用两个单引号
            } else if (upperType.equals("LONG") || upperType.equals("INTEGER") || upperType.equals("TINYINT") ||
                upperType.equals("FLOAT") || upperType.equals("DOUBLE")) {
                stringBuilder.append(defaultValue); // 数字类型默认值不加引号
            } else {
                // 其他未知类型，默认加引号。如果需要处理 CURRENT_TIMESTAMP 等关键字，需特殊处理。
                stringBuilder.append("'").append(StrUtil.replace(defaultValue, "'", "''")).append("'");
            }
            stringBuilder.append(StrUtil.SPACE);
        }

        // 处理列注释 (如果允许包含) - 这个注释是列内联的，错误信息指出是表尾的COMMENT有问题
        if (includeComment && StrUtil.isNotEmpty(tableViewRowData.getComment())) {
            // 达梦在 CREATE TABLE 时支持内联的列注释
            stringBuilder.append("COMMENT '").append(StrUtil.replace(tableViewRowData.getComment(), "'", "''")).append("'"); // 转义注释中的单引号
        }

        // 在进行编码或长度检查前，移除末尾可能存在的空格
        String columnSql = StrUtil.trimEnd(stringBuilder.toString());

        if (encode) {
            // 为用作SQL字符串字面量内部的值而转义 (例如，作为 add_column_if_not_exists 过程的参数)
            // 达梦转义单引号使用两个单引号
            columnSql = StrUtil.replace(columnSql, "'", "''");
        }

        // 对生成的列定义SQL的长度进行断言，确保不超过存储过程参数的合理限制
        // 存储过程 'add_column_if_not_exists' 的 columninfo 参数可能是 VARCHAR(200)
        // 此限制应略小于存储过程参数的大小。
        int length = StrUtil.length(columnSql);
        Assert.state(length <= 190, I18nMessageUtil.get("i18n.sql_statement_too_long.38d6") +
            " 生成的列SQL定义长度为 " + length + " 字符。用于存储过程参数时最大约为190。");
        return columnSql;
    }

    @Override
    public String delimiter() {
        return "-- dameng delimiter";
    }
}
