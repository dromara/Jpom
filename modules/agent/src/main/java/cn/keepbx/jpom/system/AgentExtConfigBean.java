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
     * 单例
     *
     * @return this
     */
    public static AgentExtConfigBean getInstance() {
        return SpringUtil.getBean(AgentExtConfigBean.class);
    }
}
