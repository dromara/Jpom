package org.dromara.jpom.configuration;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dromara.jpom.common.Const;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("jpom.cluster")
public class ClusterConfig {

    /**
     * 集群Id，默认为 default 不区分大小写，只能是字母或者数字，长度小于 20
     */
    private String id;
    /**
     * 检查节点心跳间隔时间,最小值 5 秒
     */
    private int heartSecond = 30;

    public int getHeartSecond() {
        return Math.max(this.heartSecond, 5);
    }

    public String getId() {
        return StrUtil.emptyToDefault(this.id, Const.WORKSPACE_DEFAULT_ID).toUpperCase();
    }
}
