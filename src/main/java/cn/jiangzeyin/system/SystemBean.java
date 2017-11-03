package cn.jiangzeyin.system;
/**
 * Created by jiangzeyin on 2017/1/5.
 */

import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author jiangzeyin
 * @create 2017 01 05 21:10
 */
@Configuration
public class SystemBean implements EnvironmentAware {
    private volatile static SystemBean systemBean;
    /**
     * 系统标示
     */
    public static String SYSTEM_TAG = "";
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 模板文件存放路径
     */
    @Value("${spring.velocity.resource-loader-path:}")
    public String VelocityPath;
    /**
     * 模板文件后缀
     */
    @Value("${spring.velocity.suffix:}")
    public String velocitySuffix;
//    /**
//     * 系统标示
//     */
//    @Value("${server.tag}")
//    public String systemTag;

    /**
     * 检测请求超时记录时间
     */
    @Value("${request_timeout_log:3000}")
    public Long request_timeout_log;
    /**
     * 系统预加载包名
     */
    @Value("${server.initPackageName:cn.jiangzeyin.system.init}")
    public String initPackageName;
    /**
     * tomcat 路径
     */
    @Value("${server.tomcat.basedir:}")
    private String tomcatBaseDir;
    /**
     * 程序绑定域名
     */
    @Value("${server.domain:}")
    public String domain;
    /**
     * 程序运行模式
     */
    @Value("${spring.profiles.active:dev}")
    private String profiles_active;
    /**
     *
     */
    @Value("${server.api.token:}")
    public String systemApiToken;


    /**
     * 系统配置对象
     *
     * @return
     */
    public static SystemBean getInstance() {
        if (systemBean == null) {
            synchronized (SystemBean.class) {
                if (systemBean == null) {
                    systemBean = SpringUtil.getBean(SystemBean.class);
                }
            }
        }
        return systemBean;
    }

    public Environment getEnvironment() {
        return environment;
    }


    public String getTomcatBaseDir() {
        return tomcatBaseDir;
    }
}
