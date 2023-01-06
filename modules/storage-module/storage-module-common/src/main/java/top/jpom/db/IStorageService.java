package top.jpom.db;

import cn.hutool.db.ds.DSFactory;
import io.jpom.system.JpomRuntimeException;

import java.io.File;
import java.sql.SQLException;

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
     * 获取数据库连接工厂
     *
     * @return DSFactory
     */
    DSFactory getDsFactory();

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
        if (recoverSqlFile == null) {
            return;
        }
        throw new IllegalArgumentException("没有实现改功能");
    }

    /**
     * 修改账号 密码
     *
     * @param oldUes 旧的账号
     * @param newUse 新的账号
     * @param newPwd 新密码
     */
    default void alterUser(String oldUes, String newUse, String newPwd) throws SQLException {
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
