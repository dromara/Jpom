package cn.keepbx.jpom.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.script.ScriptUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.JpomApplication;
import cn.keepbx.jpom.model.JpomManifest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
        File file = JpomApplication.getRunPath();
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
     * 控制台日志保存时长单位天
     */
    @Value("${log.saveDays:7}")
    private int logSaveDays;
    /**
     * 项目运行存储路径
     */
    @Value("${jpom.path}")
    private String path;
    /**
     * 当ip连续登录失败，锁定对应IP时长，单位毫秒
     */
    @Value("${user.ipErrorLockTime:60*60*5*1000}")
    private String ipErrorLockTime;
    private long ipErrorLockTimeValue = -1;

    public long getIpErrorLockTime() {
        if (this.ipErrorLockTimeValue == -1) {
            String str = StrUtil.emptyToDefault(this.ipErrorLockTime, "60*60*5*1000");
            this.ipErrorLockTimeValue = Convert.toLong(ScriptUtil.eval(str), TimeUnit.HOURS.toMillis(5));
        }
        return this.ipErrorLockTimeValue;
    }

    public String getPath() {
        if (StrUtil.isEmpty(path)) {
            if (JpomManifest.getInstance().isDebug()) {
                // 调试模式 为根路径的 jpom文件
                path = "/jpom/";
            } else {
                // 获取当前项目运行路径的父级
                File file = JpomApplication.getRunPath();
                if (!file.exists() && !file.isFile()) {
                    throw new RuntimeException("请配置运行路径属性【jpom.path】");
                }
                path = file.getParentFile().getParentFile().getAbsolutePath();
            }
        }
        return path;
    }

    /**
     * 配置错误或者没有，默认是7天
     *
     * @return int
     */
    public int getLogSaveDays() {
        if (logSaveDays <= 0) {
            return 7;
        }
        return logSaveDays;
    }

    /**
     * 单例
     *
     * @return this
     */
    public static ExtConfigBean getInstance() {
        return SpringUtil.getBean(ExtConfigBean.class);
    }
}
