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

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.exceptions.CheckedUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.util.ServiceLoaderUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/1/5
 */
@Slf4j
public class StorageTableFactory {

    /**
     * 初始化表 sql
     *
     * @param resource 资源
     * @return sql
     */
    public static String initTable(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader csvReader = CsvUtil.getReader(csvReadConfig);
            BufferedReader bufferedReader = IoUtil.getUtf8Reader(inputStream);
            List<TableViewData> tableViewData = csvReader.read(bufferedReader, TableViewData.class);
            tableViewData = tableViewData.stream()
                .peek(data -> data.setTableName(StorageServiceFactory.getInstance().parseRealTableName(data.getTableName())))
                .collect(Collectors.toList());

            Map<String, List<TableViewData>> map = CollStreamUtil.groupByKey(tableViewData, TableViewData::getTableName);
            StringBuilder stringBuffer = new StringBuilder();

            IStorageSqlBuilderService sqlBuilderService = StorageTableFactory.get();
            for (Map.Entry<String, List<TableViewData>> entry : map.entrySet()) {
                String key = entry.getKey();
                List<TableViewData> value = entry.getValue();
                String tableComment = value.stream()
                    .map(TableViewData::getTableComment)
                    .filter(StrUtil::isNotEmpty)
                    .findAny()
                    .orElse(null);
                Assert.hasText(tableComment, key + I18nMessageUtil.get("i18n.no_description.c231"));
                stringBuffer.append(sqlBuilderService.generateTableSql(key, tableComment, value)).append(StrUtil.LF);
                stringBuffer.append(sqlBuilderService.delimiter()).append(StrUtil.LF);
            }
            return stringBuffer.toString();
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    public static String initAlter(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader csvReader = CsvUtil.getReader(csvReadConfig);
            BufferedReader bufferedReader = IoUtil.getUtf8Reader(inputStream);
            List<TableViewAlterData> tableViewData = csvReader.read(bufferedReader, TableViewAlterData.class);
            tableViewData = tableViewData.stream()
                .peek(data -> data.setTableName(StorageServiceFactory.getInstance().parseRealTableName(data.getTableName())))
                .collect(Collectors.toList());
            IStorageSqlBuilderService sqlBuilderService = StorageTableFactory.get();
            return sqlBuilderService.generateAlterTableSql(tableViewData);

        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    public static String initIndex(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
            csvReadConfig.setHeaderLineNo(0);
            CsvReader csvReader = CsvUtil.getReader(csvReadConfig);
            BufferedReader bufferedReader = IoUtil.getUtf8Reader(inputStream);
            List<TableViewIndexData> tableViewData = csvReader.read(bufferedReader, TableViewIndexData.class);
            tableViewData = tableViewData.stream()
                .peek(data -> data.setTableName(StorageServiceFactory.getInstance().parseRealTableName(data.getTableName())))
                .collect(Collectors.toList());
            IStorageSqlBuilderService sqlBuilderService = StorageTableFactory.get();
            return sqlBuilderService.generateIndexSql(tableViewData);

        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    /**
     * 获得单例的 IStorageSqlBuilderService
     *
     * @return 单例的 IStorageSqlBuilderService
     */
    public static IStorageSqlBuilderService get() {
        Assert.notNull(StorageServiceFactory.getInstance().getMode(), I18nMessageUtil.get("i18n.unknown_database_mode.f9e5"));
        return Singleton.get(IStorageSqlBuilderService.class.getName(), (CheckedUtil.Func0Rt<IStorageSqlBuilderService>) () -> doCreateStorageService(StorageServiceFactory.getInstance().getMode()));
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static IStorageSqlBuilderService doCreateStorageService(DbExtConfig.Mode mode) {
        final List<IStorageSqlBuilderService> storageServiceList = ServiceLoaderUtil.loadList(IStorageSqlBuilderService.class);
        if (storageServiceList != null) {
            for (IStorageSqlBuilderService storageService : storageServiceList) {
                if (storageService.mode() == mode) {
                    return storageService;
                }
            }
        }
        throw new RuntimeException("No Jpom Storage " + mode + " jar found ! Please add one of it to your project !");
    }
}
