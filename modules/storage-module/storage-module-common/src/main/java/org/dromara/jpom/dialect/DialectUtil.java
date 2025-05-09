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

import cn.hutool.db.dialect.Dialect;
import cn.hutool.db.dialect.impl.DmDialect;
import cn.hutool.db.dialect.impl.H2Dialect;
import cn.hutool.db.dialect.impl.MysqlDialect;
import cn.hutool.db.dialect.impl.PostgresqlDialect;
import cn.hutool.db.sql.Wrapper;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
    /**
     * 通用的包裹器
     */
    private static final Wrapper COMMON_WRAPPER = new Wrapper('`');

    public static Dialect getH2Dialect() {
        return DIALECT_CACHE.computeIfAbsent(DbExtConfig.Mode.H2, key -> {
            H2Dialect h2Dialect = new H2Dialect();
            h2Dialect.setWrapper(COMMON_WRAPPER);
            return h2Dialect;
        });
    }

    public static Dialect getMySqlDialect() {
        return DIALECT_CACHE.computeIfAbsent(DbExtConfig.Mode.MYSQL, key -> new MysqlDialect());
    }

    public static Dialect getDmDialect() {
        return DIALECT_CACHE.computeIfAbsent(DbExtConfig.Mode.DAMENG, key -> new DmDialect());
    }

    /**
     * 获取自定义postgresql数据库的方言
     *
     * @return 自定义postgresql数据库的方言
     */
    public static Dialect getPostgresqlDialect() {
        return DIALECT_CACHE.computeIfAbsent(DbExtConfig.Mode.POSTGRESQL, key -> {
            Set<String> names = Stream.of("group", "desc", "user", "content")
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
            Wrapper wrapper = new Wrapper('"') {
                @Override
                public String wrap(String field) {
                    String unWrap = COMMON_WRAPPER.unWrap(field);
                    // 代码中存在固定编码 warp
                    if (names.contains(unWrap)) {
                        return super.wrap(unWrap);
                    }
                    // 不属于names的直接返回 并且转小写
                    return unWrap.toLowerCase();
                }

                @Override
                public String unWrap(String field) {
                    String unWrap = COMMON_WRAPPER.unWrap(field);
                    return super.unWrap(unWrap);
                }
            };
            PostgresqlDialect dialect = new PostgresqlDialect();
            dialect.setWrapper(wrapper);
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
            case DAMENG:
                return getDmDialect();
            default:
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unknown_database_dialect_type.951b") + mode);
        }
    }

    /**
     * 获取表名或列名在当前配置的数据库上的方言,例如 postgresql会使用 " ,mysql使用 `
     *
     * @param field 表名或者列名
     * @return 处理后的表名或者列名
     */
    public static String wrapField(String field) {
        initWrapFieldMap();
        return currentDbFieldWrapper.wrap(field);
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
                        throw new IllegalStateException(I18nMessageUtil.get("i18n.database_mode_config_missing.ae5d"));
                    }
                    Dialect dialectByMode = getDialectByMode(dbExtConfig.getMode());
                    currentDbFieldWrapper = dialectByMode.getWrapper();
                }
            }
        }
    }

    public static String unWrapField(String field) {
        initWrapFieldMap();
        return currentDbFieldWrapper.unWrap(field);
    }
}
