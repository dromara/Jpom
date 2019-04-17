package cn.keepbx.jpom.system;

import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Configuration
public class AgentExtConfigBean {
    /**
     * 白名单路径是否判断包含关系
     */
    @Value("${whitelistDirectory.checkStartsWith:true}")
    public boolean whitelistDirectoryCheckStartsWith;

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
    public static AgentExtConfigBean getInstance() {
        return SpringUtil.getBean(AgentExtConfigBean.class);
    }
}
