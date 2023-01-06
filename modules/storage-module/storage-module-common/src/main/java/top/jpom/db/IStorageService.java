package top.jpom.db;

import cn.hutool.db.ds.DSFactory;
import io.jpom.system.JpomRuntimeException;

import java.io.File;

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
     * @param dbExtConfig 配置参数信息
     * @return 数据库连接工厂
     */
    DSFactory create(DbExtConfig dbExtConfig);

    /**
     * 恢复数据库
     *
     * @return 恢复后的 sql 路径
     * @throws Exception 异常
     */
    default File recoverDb() throws Exception {
        throw new IllegalArgumentException("没有实现改功能");
    }

    /**
     * 删除数据库
     *
     * @return 删除后的 sql 路径
     * @throws Exception 异常
     */
    default String deleteDbFiles() throws Exception {
        throw new IllegalArgumentException("没有实现改功能");
    }

    /**
     * 转换 sql 文件内容,低版本兼容高版本
     *
     * @param sqlFile sql 文件
     */
    default void transformSql(File sqlFile) {
        throw new IllegalArgumentException("没有实现改功能");
    }

    /**
     * 恢复数据库
     *
     * @param dsFactory      数据库连接
     * @param recoverSqlFile 要恢复的数据库文件
     * @throws Exception 异常
     */
    default void executeRecoverDbSql(DSFactory dsFactory, File recoverSqlFile) throws Exception {
        throw new IllegalArgumentException("没有实现改功能");
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
