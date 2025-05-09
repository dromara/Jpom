/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.CaseInsensitiveMap;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.*;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.db.IStorageService;
import org.dromara.jpom.db.StorageServiceFactory;
import org.dromara.jpom.db.TableName;
import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SELECT * FROM INFORMATION_SCHEMA.COLUMNS  where TABLE_SCHEMA='PUBLIC'
 * <p>
 * SELECT * FROM INFORMATION_SCHEMA.INDEXES where index_type_name='UNIQUE INDEX'
 *
 * @author bwcx_jzy
 * @since 2023/1/5
 */
@Slf4j
public class H2TableToCsv2Test extends ApplicationStartTest {

    @Test
    public void init() throws SQLException {
        Set<Class<?>> classes = ClassUtil.scanPackageByAnnotation("org.dromara.jpom", TableName.class);
        Map<String, Map<String, Field>> TABLE_NAME_MAP = CollStreamUtil.toMap(classes, aClass -> {
            TableName tableName = aClass.getAnnotation(TableName.class);
            return StorageServiceFactory.getInstance().parseRealTableName(tableName);
        }, aClass -> {
            Map<String, Field> fieldMap = ReflectUtil.getFieldMap(aClass);
            return new CaseInsensitiveMap<>(fieldMap);
        });
        IStorageService iStorageService = StorageServiceFactory.getInstance().get();
        List<Entity> query = Db.use(iStorageService.getDsFactory().getDataSource()).query("SELECT * FROM INFORMATION_SCHEMA.COLUMNS  where TABLE_SCHEMA='PUBLIC'");

        Map<String, List<JSONObject>> listMap = query.stream().map(entity -> {
            JSONObject jsonObject = new JSONObject();
            String remarks = entity.getStr("remarks");
            String columnName = entity.getStr("column_name");
            String dataType = entity.getStr("data_type");
            String tableName = entity.getStr("table_name");
            String columnDefault = entity.getStr("column_default");
            String isNullable = entity.getStr("is_nullable");
//                String is_identity = entity.getStr("is_identity");
            Integer character_octet_length = entity.getInt("character_octet_length");
//                log.debug("{}-{}【{}】 {} {} {} {} {}", tableName, columnName, remarks, dataType, character_octet_length, columnDefault, isNullable, is_identity);
            Map<String, Field> stringFieldMap = TABLE_NAME_MAP.get(tableName);
            Assert.notNull(stringFieldMap, "没有找到对应的表名" + tableName);
            Field field = stringFieldMap.get(columnName);
            Assert.notNull(field, tableName + " 没有对应的字段：" + columnName);
            //System.out.println(entity);
            jsonObject.put("tableName", tableName);
            jsonObject.put("columnName", field.getName());
            jsonObject.put("remarks", remarks);
            jsonObject.put("dataType", convertDataType(dataType));
            jsonObject.put("len", character_octet_length);
            jsonObject.put("defaultValue", columnDefault);
            jsonObject.put("isNullable", BooleanUtil.toBoolean(isNullable));
            jsonObject.put("ordinalPosition", entity.getInt("ordinal_position"));
            // dtd_identifier
            String tableRemarks = null;
            try {
                tableRemarks = this.tableRemarks(tableName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            jsonObject.put("tableRemarks", tableRemarks);

            return jsonObject;
        }).collect(Collectors.groupingBy(o -> o.getString("tableName"), Collectors.toList()));
        CsvWriter writer = CsvUtil.getWriter(new File("table.all.v1.0.csv"), CharsetUtil.CHARSET_UTF_8);
        // tableName,name,type,len,defaultValue,notNull,primaryKey,comment,tableComment
        writer.writeLine("tableName", "name", "type", "len", "defaultValue", "notNull", "primaryKey", "comment", "tableComment");
        for (List<JSONObject> list : listMap.values()) {
            list.sort(Comparator.comparing(o -> o.getInteger("ordinalPosition")));
            for (JSONObject jsonObject : list) {
                String tableName = jsonObject.getString("tableName");
                String columnName = jsonObject.getString("columnName");
                String remarks = jsonObject.getString("remarks");
                String dataType = jsonObject.getString("dataType");
                Integer len = jsonObject.getInteger("len");
                String lenStr = len == null ? StrUtil.EMPTY : len <= 0 ? StrUtil.EMPTY : len + "";
                String defaultValue = jsonObject.getString("defaultValue");
                String tableRemarks = jsonObject.getString("tableRemarks");
                Boolean nullable = jsonObject.getBoolean("isNullable");

                writer.writeLine(tableName, columnName, dataType, lenStr, defaultValue, String.valueOf(!nullable),
                    StrUtil.equals("id", columnName) ? Boolean.TRUE.toString() : Boolean.FALSE.toString(), remarks,
                    StrUtil.equals("id", columnName) ? tableRemarks : StrUtil.EMPTY);
            }
        }
        writer.flush();
        System.out.println(listMap);
    }

    private String tableRemarks(String tableName) throws SQLException {
        String sql = "SELECT * FROM INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA='PUBLIC' and  TABLE_NAME=?";
        IStorageService iStorageService = StorageServiceFactory.getInstance().get();
        List<Entity> query = Db.use(iStorageService.getDsFactory().getDataSource()).query(sql, tableName);
        Entity entity = CollUtil.getFirst(query);
        return entity.getStr("REMARKS");
    }

    private String convertDataType(String dataType) {
        switch (dataType) {
            case "BIGINT":
                return Long.class.getSimpleName();
            case "CHARACTER VARYING":
                return String.class.getSimpleName();
            case "CHARACTER LARGE OBJECT":
                return "TEXT";
            case "INTEGER":
                return Integer.class.getSimpleName();
            case "TINYINT":
                return "TINYINT";
            case "REAL":
                return Float.class.getSimpleName();
            case "DOUBLE PRECISION":
                return Double.class.getSimpleName();
            default:
                throw new IllegalArgumentException("未知的数据类型:" + dataType);
        }
    }
}
