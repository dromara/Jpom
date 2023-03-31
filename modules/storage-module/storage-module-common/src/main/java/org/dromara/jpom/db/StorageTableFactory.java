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
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

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
                Assert.hasText(tableComment, key + " 没有描述");
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
        Assert.notNull(StorageServiceFactory.getMode(), "当前数据库模式未知");
        return Singleton.get(IStorageSqlBuilderService.class.getName(), (CheckedUtil.Func0Rt<IStorageSqlBuilderService>) () -> doCreateStorageService(StorageServiceFactory.getMode()));
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
