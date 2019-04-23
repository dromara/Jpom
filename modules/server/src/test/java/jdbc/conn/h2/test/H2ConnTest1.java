/**
 *
 */
package jdbc.conn.h2.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * <p>ClassName: H2ConnTest1<p>
 * <p>Description: Java通过JDBC方式连接H2数据库<p>
 * @author xudp
 * @version 1.0 V
 * @createTime 2014-12-18 上午11:22:12
 */
public class H2ConnTest1 {
    //数据库连接URL，当前连接的是E:/H2目录下的gacl数据库
    private static final String JDBC_URL = "jdbc:h2:D:\\jpom\\server\\db\\Server";
    //连接数据库时使用的用户名
    private static final String USER = "jpom";
    //连接数据库时使用的密码
    private static final String PASSWORD = "jpom";
    //连接H2数据库时使用的驱动类，org.h2.Driver这个类是由H2数据库自己提供的，在H2数据库的jar包中可以找到
    private static final String DRIVER_CLASS = "org.h2.Driver";

    public static void main(String[] args) throws Exception {
        // 加载H2数据库驱动
        Class.forName(DRIVER_CLASS);
        // 根据连接URL，用户名，密码获取数据库连接
        Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        Statement stmt = conn.createStatement();

        //查询
        ResultSet rs = stmt.executeQuery("SELECT * FROM UserOperateLogV1");
        //遍历结果集
        while (rs.next()) {
            System.out.println(rs.getString("userId"));
        }
        //释放资源
        stmt.close();
        //关闭连接
        conn.close();
    }
}