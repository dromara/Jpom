/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.dialect;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.dialect.Dialect;
import cn.hutool.db.dialect.impl.H2Dialect;
import cn.hutool.db.dialect.impl.MysqlDialect;
import cn.hutool.db.dialect.impl.PostgresqlDialect;
import cn.hutool.db.sql.Wrapper;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.db.DbExtConfig;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据库方言工具类
 *
 * @author whz
 * @since 2024/3/10
 */
public class DialectUtil {

    private final static ConcurrentHashMap<DbExtConfig.Mode, Dialect> DIALECT_CACHE = new ConcurrentHashMap<>();
    private static volatile Wrapper currentDbFieldWrapper;
    private static final ConcurrentHashMap<String, String> WRAP_FIELD_MAP = new ConcurrentHashMap<>();

    public static Dialect getH2Dialect() {
        return DIALECT_CACHE.computeIfAbsent(DbExtConfig.Mode.H2, key -> {
            H2Dialect h2Dialect = new H2Dialect();
            h2Dialect.setWrapper(new Wrapper('`'));
            return h2Dialect;
        });
    }

    public static Dialect getMySqlDialect() {
        return DIALECT_CACHE.computeIfAbsent(DbExtConfig.Mode.MYSQL, key -> new MysqlDialect());
    }

    /**
     * 获取自定义postgresql数据库的方言
     *
     * @return 自定义postgresql数据库的方言
     */
    public static Dialect getPostgresqlDialect() {
        return DIALECT_CACHE.computeIfAbsent(DbExtConfig.Mode.POSTGRESQL, key -> {
            // 需要特殊处理的列名或表名，需要时在这里添加即可
            Set<String> names = Stream.of("group", "desc", "user", "content").map(String::toLowerCase).collect(Collectors.toSet());
            Wrapper wrapper = new Wrapper() {
                @Override
                public String wrap(String field) {
                    field = field.toLowerCase();
                    if (field.charAt(0) == '`' && field.charAt(field.length() - 1) == '`') {
                        field = field.substring(1, field.length() - 1);
                    }
                    if (names.contains(field)) {
                        return super.wrap(field);
                    }
                    return field; // 不属于names的直接返回
                }
            };

            PostgresqlDialect dialect = new PostgresqlDialect();
            Wrapper innerWrapper = (Wrapper) BeanUtil.getFieldValue(dialect, "wrapper");

            wrapper.setPreWrapQuote(innerWrapper.getPreWrapQuote());
            wrapper.setSufWrapQuote(innerWrapper.getSufWrapQuote());
            BeanUtil.setFieldValue(dialect, "wrapper", wrapper);
            return dialect;
        });
    }

    public static Dialect getDialectByMode(DbExtConfig.Mode mode) {
        switch (mode) {
            case H2:
                return getH2Dialect();
            case MYSQL:
            case MARIADB:
                return getMySqlDialect();
            case POSTGRESQL:
                return getPostgresqlDialect();
        }
        throw new IllegalArgumentException("未知的数据库方言类型");
    }

    /**
     * 获取表名或列名在当前配置的数据库上的方言,例如 postgresql会使用 " ,mysql使用 `
     *
     * @param field 表名或者列名
     * @return 处理后的表名或者列名
     */
    public static String wrapField(String field) {
        initWrapFieldMap();
        return WRAP_FIELD_MAP.computeIfAbsent(field, key -> currentDbFieldWrapper.wrap(field));
    }

    /**
     * 初始化当前数据库 wrapper
     */
    private static void initWrapFieldMap() {
        if (currentDbFieldWrapper == null) {
            synchronized (DialectUtil.class) {
                if (currentDbFieldWrapper == null) {
                    DbExtConfig dbExtConfig = SpringUtil.getBean(DbExtConfig.class);
                    if (dbExtConfig == null || dbExtConfig.getMode() == null) {
                        throw new IllegalStateException("数据库Mode配置缺失");
                    }
                    Dialect dialectByMode = getDialectByMode(dbExtConfig.getMode());
                    currentDbFieldWrapper = dialectByMode.getWrapper();
                }
            }
        }
    }

    public static String unWrapField(String field) {
        initWrapFieldMap();
        return WRAP_FIELD_MAP.computeIfAbsent(field, s -> {
            Character preWrapQuote = currentDbFieldWrapper.getPreWrapQuote();
            Character sufWrapQuote = currentDbFieldWrapper.getSufWrapQuote();
            if (preWrapQuote == null || sufWrapQuote == null || StrUtil.isBlank(s)) {
                return s;
            }

            //如果已经包含包装的引号，返回原字符
            if (!StrUtil.isSurround(s, preWrapQuote, sufWrapQuote)) {
                return s;
            }

            //如果字段中包含通配符或者括号（字段通配符或者函数），不做包装
            if (StrUtil.containsAnyIgnoreCase(s, "*", "(", " ", " as ")) {
                return s;
            }

            return StrUtil.removeSuffix(StrUtil.removePrefix(s, preWrapQuote.toString()), sufWrapQuote.toString());
        });
    }
}
