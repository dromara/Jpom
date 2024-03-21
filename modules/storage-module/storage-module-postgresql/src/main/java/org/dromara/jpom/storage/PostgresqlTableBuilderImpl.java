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
import cn.hutool.core.util.*;
import cn.hutool.db.sql.Wrapper;
import org.dromara.jpom.db.*;
import org.dromara.jpom.dialect.DialectUtil;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * postgresql的sql语句构建类
 * 目前不管列名还是表名，索引名，都会转化为小写的形式，而特殊的关键字会通过Dialect的Wrapper处理
 * @author whz
 * @since 2024/3/10
 */
public class PostgresqlTableBuilderImpl implements IStorageSqlBuilderService {

    /**
     * 记录当前所有Model中含有的bool字段的属性名，表名和属性名都会被小写处理
     */
    private final Map<String, Set<String>> tableName2ModelBoolFieldNameSetMap;

    public PostgresqlTableBuilderImpl() {
        Set<Class<?>> modelClassSet = ClassUtil.scanPackageByAnnotation("org.dromara.jpom", TableName.class);
        this.tableName2ModelBoolFieldNameSetMap = new HashMap<>();
        modelClassSet.forEach(modelClass->{
            TableName annotation = modelClass.getAnnotation(TableName.class);
            // 统一处理成小写，model也应该不会出现转为小写后重名的field
            String tableName = annotation.value().toLowerCase();
            Field[] boolFieldArr = ReflectUtil.getFields(modelClass, field -> Boolean.class.equals(field.getType()) || boolean.class.equals(field.getType()));
            Set<String> nameSet = Arrays.stream(boolFieldArr)
                .map(field -> field.getName().toLowerCase())
                .collect(Collectors.toSet());
            tableName2ModelBoolFieldNameSetMap.put(tableName,nameSet);
        });
    }

    @Override
    public DbExtConfig.Mode mode() {
        return DbExtConfig.Mode.POSTGRESQL;
    }

    @Override
    public String generateIndexSql(List<TableViewIndexData> row) {
        StringBuilder stringBuilder = new StringBuilder();
        Wrapper fieldWrapper = DialectUtil.getPostgresqlDialect().getWrapper();
        for (TableViewIndexData viewIndexData : row) {
            String indexType = viewIndexData.getIndexType();
            // 存储过程对大小写敏感，因此传入的 表名，索引名,列名都转需要为小写
            String name = viewIndexData.getName().toLowerCase();
            String field = viewIndexData.getField();
            String tableName = viewIndexData.getTableName().toLowerCase();

            List<String> fields = StrUtil.splitTrim(field, "+").stream()
                .map(fieldWrapper::wrap).collect(Collectors.toList());
            /**
             *  CALL drop_index_if_exists('表名','索引名');
             *  CREATE UNIQUE INDEX 索引名 ON 表名 (field1,field2,...);
             *  CREATE INDEX 索引名 ON 表名 (field1,field2,...);
             *  field需要通过wrapper处理
             */
            switch (indexType) {
                case "ADD-UNIQUE":{
                    Assert.notEmpty(fields, "索引未配置字段");
                    stringBuilder.append("CALL drop_index_if_exists('").append(tableName).append("','").append(name).append("')").append(";").append(StrUtil.LF);
                    stringBuilder.append(this.delimiter()).append(StrUtil.LF);
                    stringBuilder.append("CREATE UNIQUE INDEX ").append(name)
                        .append(" ON ").append(tableName).append(" (").append(CollUtil.join(fields, StrUtil.COMMA)).append(")")
                        .append(";");
                    break;
                }
                case "ADD": {
                    Assert.notEmpty(fields, "索引未配置字段");
                    stringBuilder.append("CALL drop_index_if_exists('").append(tableName).append("','").append(name).append("')").append(";").append(StrUtil.LF);
                    stringBuilder.append(this.delimiter()).append(StrUtil.LF);
                    stringBuilder.append("CREATE INDEX ").append(name)
                        .append(" ON ").append(tableName).append(" (").append(CollUtil.join(fields, StrUtil.COMMA)).append(")")
                        .append(";");
                    break;
                }
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
        Wrapper fieldWrapper = DialectUtil.getPostgresqlDialect().getWrapper();
        for (TableViewAlterData viewAlterData : row) {
            String alterType = viewAlterData.getAlterType();
            String tableName = fieldWrapper.wrap(viewAlterData.getTableName());
            String columnName = viewAlterData.getName().toLowerCase(); //不使用wrapper，存储过程调用时，column不需要包裹
            switch (alterType) {
                case "DROP":
                    stringBuilder.append("CALL drop_column_if_exists('").append(tableName).append("', '").append(columnName).append("')");
                    break;
                case "ADD":
                    stringBuilder.append("CALL add_column_if_not_exists('").append(tableName).append("','")
                        .append(columnName).append("','");
                    stringBuilder.append(generateColumnSql(viewAlterData.getTableName(),viewAlterData,true));
                    stringBuilder.append("');");
                    /**
                     *  添加列时，因为不能同时指定注释内容，单独加一个语句设置注释
                     *  COMMENT ON COLUMN 表名.field IS '注释内容'
                     *  field需要通过wrapper处理
                     */
                    if( StrUtil.isNotBlank(viewAlterData.getComment()) ){
                        stringBuilder.append(delimiter()).append(StrUtil.LF);
                        stringBuilder.append("COMMENT ON COLUMN ").append(tableName)
                            .append(StrUtil.DOT).append(fieldWrapper.wrap(viewAlterData.getName())).append(" IS ")
                            .append("'").append(viewAlterData.getComment().trim()).append("';");
                    }
                    break;
                case "ALTER":
                    stringBuilder.append(generateAlterSql(viewAlterData));
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
     * @param name 表名
     * @param desc 描述
     * @param row  字段信息
     * @return sql
     */
    @Override
    public String generateTableSql(String name, String desc, List<TableViewData> row) {
        StringBuilder stringBuilder = new StringBuilder();
        Wrapper fieldWrapper = DialectUtil.getPostgresqlDialect().getWrapper();
        String tableName = fieldWrapper.wrap(name);

        stringBuilder.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(StrUtil.LF);
        stringBuilder.append("(").append(StrUtil.LF);
        for (TableViewData tableViewData : row) {
            stringBuilder.append(StrUtil.TAB).append(this.generateColumnSql(tableName,tableViewData)).append(StrUtil.COMMA).append(StrUtil.LF);
        }
        // 主键
        List<String> primaryKeys = row.stream()
                .filter(tableViewData -> tableViewData.getPrimaryKey() != null && tableViewData.getPrimaryKey())
                .map(viewData->fieldWrapper.wrap(viewData.getName()))
                .collect(Collectors.toList());
        Assert.notEmpty(primaryKeys, "表没有主键");
        stringBuilder.append(StrUtil.TAB).append("PRIMARY KEY (").append(CollUtil.join(primaryKeys, StrUtil.COMMA)).append(")").append(StrUtil.LF);
        stringBuilder.append(");").append(StrUtil.LF);
        // 表注释
        stringBuilder.append("COMMENT ON TABLE ").append(fieldWrapper.wrap(name)).append(" IS '").append(desc).append("';");


        // 建表语句的列注释需要通过单独的sql语句设置
        for (TableViewData tableViewData : row) {
            if( StrUtil.isNotBlank(tableViewData.getComment()) ) {
                stringBuilder.append(delimiter()).append(StrUtil.LF);
                stringBuilder.append("CALL exec_if_column_exists('")
                    .append(tableName).append("','")
                    .append(tableViewData.getName()).append("','")
                    .append("COMMENT ON COLUMN ").append(tableName)
                    .append(StrUtil.DOT).append(fieldWrapper.wrap(tableViewData.getName())).append(" IS ")
                    .append("''").append(tableViewData.getComment().trim()).append("'' ');");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String generateColumnSql(TableViewRowData tableViewRowData) {
        return StrUtil.EMPTY;
    }

    @Override
    public String generateColumnSql(String tableName, TableViewRowData tableViewRowData) {
        return generateColumnSql(tableName,tableViewRowData,false);
    }

    /**
     * 生成 alter add 或 create table 时的列定义
     * @param tableName
     * @param tableViewRowData
     * @param encode
     * @return
     */
    private String generateColumnSql(String tableName, TableViewRowData tableViewRowData, boolean encode) {

        StringBuilder strBuilder = new StringBuilder();
        Wrapper fieldWrapper = DialectUtil.getPostgresqlDialect().getWrapper();
        String type = getColumnTypeStr(tableName,tableViewRowData.getName(),
            tableViewRowData.getType(),tableViewRowData.getLen());
        strBuilder.append(StrUtil.SPACE).append(fieldWrapper.wrap(tableViewRowData.getName()))
            .append(StrUtil.SPACE).append(type);

        String defaultValue = tableViewRowData.getDefaultValue();
        if (StrUtil.isNotEmpty(defaultValue)) {
            if( "BOOLEAN".equals(type) ) {
                defaultValue = Boolean.toString(BooleanUtil.toBoolean(defaultValue.trim()));
            }
            strBuilder.append(" DEFAULT '").append(defaultValue).append("'");
        }

        Boolean notNull = tableViewRowData.getNotNull();
        if (notNull != null && notNull) {
            strBuilder.append(" NOT NULL ");
        }
        String columnSql = strBuilder.toString();
        columnSql = encode ? StrUtil.replace(columnSql, "'", "''") : columnSql;
        int length = StrUtil.length(columnSql);
        Assert.state(length <= 180, "sql 语句太长啦");
        return columnSql;
    }

    /**
     * 生成postgresql的alter语句
     * postgresql不像 h2或mysql可以一个alter同时设置 数据类型，默认值，非空，注释，因此需生成多条sql语句才能实现功能
     * @param viewAlterData
     * @return
     */
    private String generateAlterSql(TableViewAlterData viewAlterData) {

        Wrapper fieldWrapper = DialectUtil.getPostgresqlDialect().getWrapper();
        StringBuilder strBuilder = new StringBuilder();
        String tableName = fieldWrapper.wrap(viewAlterData.getTableName());
        String name = fieldWrapper.wrap(viewAlterData.getName());

        // 先改类型
        String type = getColumnTypeStr(viewAlterData.getTableName(),viewAlterData.getName()
            ,viewAlterData.getType(),viewAlterData.getLen());
        strBuilder.append("ALTER TABLE ").append(tableName)
            .append(" ALTER COLUMN ").append(name)
            .append(" TYPE ").append(type).append(";");

        // 再设置默认值
        strBuilder.append(delimiter()).append(StrUtil.LF);
        String defaultValue = viewAlterData.getDefaultValue();
        strBuilder.append("ALTER TABLE ").append(tableName)
            .append(" ALTER COLUMN  ").append(name)
            .append(" SET DEFAULT '");
        if (StrUtil.isNotEmpty(defaultValue)) {
            if( "BOOLEAN".equals(type) ) {
                defaultValue = Boolean.toString(BooleanUtil.toBoolean(defaultValue.trim()));
            }
            strBuilder.append(defaultValue).append("';");
        }else {
            strBuilder.append("NULL").append("';");
        }

        // 设置非空
        strBuilder.append(delimiter()).append(StrUtil.LF);
        strBuilder.append("ALTER TABLE ").append(tableName)
            .append(" ALTER COLUMN  ").append(name);
        Boolean notNull = viewAlterData.getNotNull();
        if (notNull != null && notNull) {
            strBuilder.append(" SET NOT NULL ").append(";");
        }else {
            strBuilder.append(" DROP NOT NULL ").append(";");
        }

        // 注释
        strBuilder.append(delimiter()).append(StrUtil.LF);
        String comment = viewAlterData.getComment();
        comment = StrUtil.isEmpty(comment) ? StrUtil.EMPTY : comment.trim();
        strBuilder.append("COMMENT ON COLUMN ").append(tableName)
            .append(StrUtil.DOT).append(name).append(" IS ")
            .append("'").append(comment).append("';");

        Assert.state(strBuilder.length() <= 1000, "sql 语句太长啦");
        return strBuilder.toString();
    }



    @Override
    public String delimiter() {
        return "-- postgresql $delimiter$";
    }


    private String getColumnTypeStr(String tableName,String columnName,String type,Integer dataLen) {
        Assert.hasText(type, "未正确配置数据类型");
        type = type.toUpperCase();
        switch (type) {
            case "LONG":
                return "BIGINT";
            case "STRING":
                return "VARCHAR(" + ObjectUtil.defaultIfNull(dataLen, 255) + ")";
            case "TEXT":
                return "TEXT";
            case "INTEGER":
                return "INTEGER";
            case "TINYINT": {
                columnName = columnName.toLowerCase();
                tableName = tableName.toLowerCase();
                Set<String> nameSet = tableName2ModelBoolFieldNameSetMap.get(tableName);
                return nameSet != null && nameSet.contains(columnName) ? "BOOLEAN" : "SMALLINT";
            }
            case "FLOAT":
                return "REAL";
            case "DOUBLE":
                return "DOUBLE PRECISION";
            default:
                throw new IllegalArgumentException("不支持的数据类型:" + type);
        }
    }
}
