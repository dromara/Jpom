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
package io.jpom.system.init;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.ds.DSFactory;
import cn.hutool.db.ds.GlobalDSFactory;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.service.h2db.TableName;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Auto import build.json data to DB
 *
 * @author Hotstrip
 * @since 2021-08-02
 */
@Slf4j
public class LoadBuildJsonToDB {
    private LoadBuildJsonToDB() {

    }

    /**
     * 静态内部类实现单例模式
     */
    public static class LoadBuildJsonToDBHolder {
        private static final LoadBuildJsonToDB INSTANCE = new LoadBuildJsonToDB();
    }

    public static LoadBuildJsonToDB getInstance() {
        return LoadBuildJsonToDBHolder.INSTANCE;
    }

    /**
     * read build.json file to list
     * and then use list transfer SQL and execute it
     */
    public void doJsonToSql() {
        File backupOldData = FileUtil.file(ConfigBean.getInstance().getDataPath(), "backup_old_data");
        // 读取 build.json 文件内容
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.BUILD);
        List<JSONObject> list = readBuildJsonFileToList(file);
        // 判断 list 是否为空
        if (null == list) {
            if (!FileUtil.exist(FileUtil.file(backupOldData, ServerConfigBean.BUILD))) {
                //log.warn("There is no any data, the build.json file maybe no content or file is not exist...");
            }
            return;
        }
        // 转换成 SQL 执行
        initSql(list);
        // 将 json 文件转移到备份目录
        FileUtil.move(file, FileUtil.mkdir(backupOldData), true);
        log.info("{} mv to {}", FileUtil.getAbsolutePath(file), FileUtil.getAbsolutePath(backupOldData));
    }

    /**
     * list data to SQL
     * this method is core logic
     * 1. load fields will be insert into database from class with reflection
     * 2. iterate list data, and transfer each element to SQL (element's properties mapping to SQL param name and value)
     * 3. use param map generate SQL
     * 4. exec SQL
     * ---------------------
     * 这个方法是核心逻辑
     * 1.通过反射加载字段，将其插入到数据库中
     * 2. 迭代列表数据，并将每个元素转移到参数集合（元素的属性映射到SQL参数名称和值）
     * 3. 使用参数集合对象生成SQL
     * 4. 执行SQL
     *
     * @param list data from build.json
     */
    private void initSql(List<JSONObject> list) {
        // 加载类里面的属性，用反射获取
        final List<String> repositoryFieldList = getClassFieldList(RepositoryModel.class);
        final List<String> buildInfoFieldList = getClassFieldList(BuildInfoModel.class);
        final Map<String, String> repositoryCache = new HashMap<>(list.size());

        // 遍历对象集合
        list.forEach(buildModelVo -> {
            log.debug("buildModelVo: {}", JSON.toJSONString(buildModelVo));

            // 拿到构造 SQL 的参数
            String gitUrl = buildModelVo.getString("gitUrl");
            //buildModelVo.getGitUrl();
            String repositoryId = repositoryCache.get(gitUrl);
            if (StrUtil.isEmpty(repositoryId)) {
                // 先存储仓库信息
                Map<String, Object> repositoryParamMap = initSqlParamMap(repositoryFieldList, buildModelVo);
                // add def protocol
                repositoryParamMap.put("PROTOCOL", GitProtocolEnum.HTTP.getCode());
                // 构造 insert SQL 语句
                String insertRepositorySql = initInsertSql(repositoryParamMap, RepositoryModel.class);
                // 插入数据库
                insertToDB(insertRepositorySql);
                repositoryId = (String) repositoryParamMap.get("ID");
                // cache
                repositoryCache.put(gitUrl, repositoryId);
            }

            Map<String, Object> buildInfoParamMap = initSqlParamMap(buildInfoFieldList, buildModelVo);
            // 绑定仓库ID
            buildInfoParamMap.put("REPOSITORYID", repositoryId);
            // 构建发布操作信息
            JSONObject jsonObject = new JSONObject();
            String releaseMethodDataId = buildModelVo.getString("releaseMethodDataId");
            jsonObject.put("releaseMethodDataId", releaseMethodDataId);
            jsonObject.put("afterOpt", buildModelVo.getInteger("afterOpt"));
            jsonObject.put("clearOld", buildModelVo.getBoolean("clearOld"));
            jsonObject.put("releaseCommand", buildModelVo.getString("releaseCommand"));
            jsonObject.put("releasePath", buildModelVo.getString("releasePath"));
            // 保存信息
            buildInfoParamMap.put("EXTRADATA", jsonObject.toJSONString());
            buildInfoParamMap.put("RELEASEMETHODDATAID", releaseMethodDataId);
            String insertBuildInfoSql = initInsertSql(buildInfoParamMap, BuildInfoModel.class);

            insertToDB(insertBuildInfoSql);
        });
    }

    /**
     * exec insert SQL to DB
     *
     * @param sql SQL for insert
     */
    private void insertToDB(String sql) {
        DSFactory dsFactory = GlobalDSFactory.get();
        int rows = 0;
        try {
            rows = Db.use(dsFactory.getDataSource()).execute(sql);
        } catch (SQLException e) {
            log.warn("exec SQL: {} failed", sql, e);
        }
        log.info("exec SQL: {} complete, and affected rows is: {}", sql, rows);
    }

    /**
     * init insert SQL with param map and table name
     *
     * @param paramMap
     * @param clazz    实体类
     * @return
     */
    private String initInsertSql(Map<String, Object> paramMap, Class<?> clazz) {
        TableName tableName = clazz.getAnnotation(TableName.class);
        Assert.notNull(tableName, "not find table name");
        // 构造 insert SQL 语句
        StringBuffer sqlBuffer = new StringBuffer("merge into {} ( ");
        StringBuilder sqlFieldNameBuffer = new StringBuilder();
        StringBuilder sqlFieldValueBuffer = new StringBuilder();
        for (int i = 0; i < paramMap.size(); i++) {
            sqlFieldNameBuffer.append("`{}`,");
            sqlFieldValueBuffer.append("'{}',");
        }
        sqlBuffer.append(sqlFieldNameBuffer.substring(0, sqlFieldNameBuffer.length() - 1))
            .append(" )")
            .append(" values ( ")
            .append(sqlFieldValueBuffer.substring(0, sqlFieldValueBuffer.length() - 1))
            .append(" )");

        // 构造 SQL 参数
        List<Object> params = new ArrayList<>();
        params.add(tableName.value());
        params.addAll(paramMap.keySet());
        params.addAll(paramMap.values());
        return StrUtil.format(sqlBuffer, params.toArray());
    }

    /**
     * init param map for create insert SQL
     *
     * @param fieldList  字段名 list
     * @param jsonObject json
     * @return map key value
     */
    private Map<String, Object> initSqlParamMap(List<String> fieldList, JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>(fieldList.size());

        fieldList.forEach(fieldName -> {
            // 判断类里面是否有这个属性
            Object filedValue = jsonObject.get(fieldName);
            if (filedValue == null) {
                return;
            }
            // 添加到参数对象中
            String sqlFiledName = fieldName.toUpperCase();
            map.put(sqlFiledName, filedValue);
        });
        // 同步数据创建时间
        String modifyTime = jsonObject.getString("modifyTime");
        if (StrUtil.isNotEmpty(modifyTime)) {
            map.put("CREATETIMEMILLIS", DateUtil.parse(modifyTime).getTime());
        }
        return map;
    }

    /**
     * read build.json file to list
     *
     * @return List<BuildModelVo>
     */
    private List<JSONObject> readBuildJsonFileToList(File file) {
        if (!file.exists()) {
            log.debug("there is no build.json file...");
            return null;
        }
        try {
            // 读取 build.json 文件里面的内容，转换成实体对象集合
            JSONObject jsonObject = (JSONObject) JsonFileUtil.readJson(file.getAbsolutePath());
            return jsonObject.keySet().stream()
                .map(jsonObject::get)
                .flatMap((Function<Object, Stream<JSONObject>>) o -> Stream.of((JSONObject) o))
                .collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            log.error("read build.json file failed...caused: {}...message: {}", e.getCause(), e.getMessage());
        }
        return null;
    }

    /**
     * 获取 clazz 类里面的属性，转换成集合返回
     *
     * @param clazz 实体类
     * @return List<String>
     */
    private List<String> getClassFieldList(Class<?> clazz) {
        final Field[] fields = ReflectUtil.getFieldsDirectly(clazz, true);
        return Arrays.stream(fields)
            .filter(field -> Modifier.isPrivate(field.getModifiers()))
            .flatMap(field -> Arrays.stream(new String[]{field.getName()}))
            .collect(Collectors.toList());
    }
}
