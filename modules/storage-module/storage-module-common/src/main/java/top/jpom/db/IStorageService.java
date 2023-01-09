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
package top.jpom.db;

import cn.hutool.db.ds.DSFactory;
import cn.hutool.setting.Setting;
import io.jpom.system.JpomRuntimeException;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库实现
 *
 * @author bwcx_jzy
 * @since 2023/1/5
 */
public interface IStorageService extends AutoCloseable, IMode {

    /**
     * 初始化数据库
     *
     * @param dbExtConfig 配置参数信息
     * @return 数据库连接工厂
     */
    DSFactory init(DbExtConfig dbExtConfig);

    /**
     * 创建数据库连接工厂
     *
     * @param dbExtConfig 数据库配置
     * @param url         url
     * @param user        用户名
     * @param pass        密码
     * @return 数据库连接工厂
     */
    DSFactory create(DbExtConfig dbExtConfig, String url, String user, String pass);

    /**
     * 创建数据库配置参数
     *
     * @param dbExtConfig 数据库配置
     * @param url         url
     * @param user        用户名
     * @param pass        密码
     * @return 配置
     */
    Setting createSetting(DbExtConfig dbExtConfig, String url, String user, String pass);

    /**
     * 获取数据库连接工厂
     *
     * @return DSFactory
     */
    DSFactory getDsFactory();

    /**
     * 是否存在数据库文件
     *
     * @return true 存在
     * @throws Exception 异常
     */
    default boolean hasDbData() throws Exception {
        throw new UnsupportedOperationException("没有实现该功能");
    }

    /**
     * 恢复数据库
     *
     * @return 恢复后的 sql 路径
     * @throws Exception 异常
     */
    default File recoverDb() throws Exception {
        throw new UnsupportedOperationException("没有实现该功能");
    }

    /**
     * 删除数据库
     *
     * @return 删除后的 sql 路径
     * @throws Exception 异常
     */
    default String deleteDbFiles() throws Exception {
        throw new UnsupportedOperationException("没有实现该功能");
    }

    /**
     * 转换 sql 文件内容,低版本兼容高版本
     *
     * @param sqlFile sql 文件
     */
    default void transformSql(File sqlFile) {
        throw new UnsupportedOperationException("没有实现该功能");
    }

    /**
     * 恢复数据库
     *
     * @param dsFactory      数据库连接
     * @param recoverSqlFile 要恢复的数据库文件
     * @throws Exception 异常
     */
    default void executeRecoverDbSql(DSFactory dsFactory, File recoverSqlFile) throws Exception {
        if (recoverSqlFile == null) {
            return;
        }
        throw new UnsupportedOperationException("没有实现该功能");
    }

    /**
     * 修改账号 密码
     *
     * @param oldUes 旧的账号
     * @param newUse 新的账号
     * @param newPwd 新密码
     * @throws SQLException sql 异常
     */
    default void alterUser(String oldUes, String newUse, String newPwd) throws SQLException {
        throw new UnsupportedOperationException("没有实现该功能");
    }

    /**
     * 备份数据库
     *
     * @param url           url
     * @param user          账号
     * @param pass          密码
     * @param backupSqlPath sql 存放路径
     * @param tableName     备份的表名
     * @throws Exception 异常
     */
    default void backupSql(String url, String user, String pass, String backupSqlPath, List<String> tableName) throws Exception {
        throw new UnsupportedOperationException("没有实现该功能");
    }

    /**
     * 数据库地址
     *
     * @return url
     */
    String dbUrl();

    /**
     * 异常转换
     *
     * @param e 异常
     * @return 转换后
     */
    JpomRuntimeException warpException(Exception e);
}
