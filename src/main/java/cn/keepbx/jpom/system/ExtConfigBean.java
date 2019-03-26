package cn.keepbx.jpom.system;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationHome;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;

/**
 * 外部配置文件
 *
 * @author jiangzeyin
 * @date 2019/3/04
 */
@Configuration
public class ExtConfigBean {

    static final String FILE_NAME = "extConfig.yml";

    private static Resource resource;

    /**
     * 动态获取外部配置文件的 resource
     *
     * @return File
     */
    public static Resource getResource() {
        if (resource != null) {
            return resource;
        }
        ApplicationHome home = new ApplicationHome(ExtConfigBean.class);
        String path = (home.getSource() == null ? "" : home.getSource().getAbsolutePath());
        File file = new File(path);
        if (file.isFile()) {
            file = file.getParentFile().getParentFile();
            file = new File(file, FILE_NAME);
            if (file.exists() && file.isFile()) {
                resource = new FileSystemResource(file);
                return ExtConfigBean.resource;
            }
        }
        resource = new ClassPathResource("/bin/" + FILE_NAME);
        return ExtConfigBean.resource;
    }

    /**
     * 白名单路径是否判断包含关系
     */
    @Value("${whitelistDirectory.checkStartsWith:true}")
    public boolean whitelistDirectoryCheckStartsWith;

    /**
     * 系统最多能创建多少用户
     */
    @Value("${user.maxCount:10}")
    public int userMaxCount;
    /**
     * 用户连续登录失败次数，超过此数将自动不再被允许登录，零是不限制
     */
    @Value("${user.alwaysLoginError:5}")
    public int userAlwaysLoginError;
    /**
     * 自动备份控制台日志，防止日志文件过大，目前暂只支持linux 不停服备份  如果配置none 则不自动备份 默认10分钟扫描一次
     */
    @Value("${log.autoBackConsoleCron:0 0/10 * * * ?}")
    public String autoBackConsoleCron;
    /**
     * 当文件多大时自动备份
     *
     * @see ch.qos.logback.core.util.FileSize
     */
    @Value("${log.autoBackSize:50MB}")
    public String autoBackSize;
    /**
     * 项目运行存储路径
     */
    @Value("${jpom.path}")
    private String path;

    /**
     * 标记是否为安全模式
     */
    @Value("${jpom.safeMode:false}")
    public boolean safeMode;


    public String getPath() {
        if (StrUtil.isEmpty(path)) {
            throw new RuntimeException("请配运行路径属性【jpom.path】");
        }
        return path;
    }

    /**
     * 单利
     *
     * @return this
     */
    public static ExtConfigBean getInstance() {
        return SpringUtil.getBean(ExtConfigBean.class);
    }
}
