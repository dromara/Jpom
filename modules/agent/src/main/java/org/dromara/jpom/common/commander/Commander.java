package org.dromara.jpom.common.commander;

import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.system.JpomRuntimeException;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author bwcx_jzy
 * @since 23/12/29 029
 */
@Configuration
@Slf4j
public class Commander {

    public Commander() {
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isLinux()) {
            // Linux系统
            log.debug("当前系统为：linux");
        } else if (osInfo.isWindows()) {
            // Windows系统
            log.debug("当前系统为：windows");
        } else if (osInfo.isMac()) {
            log.debug("当前系统为：mac");
        } else {
            throw new JpomRuntimeException("不支持的：" + osInfo.getName());
        }
    }

    public static class Windows implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return SystemUtil.getOsInfo().isWindows();
        }
    }

    public static class Linux implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return SystemUtil.getOsInfo().isLinux();
        }
    }

    public static class Mac implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return SystemUtil.getOsInfo().isMac();
        }
    }
}
