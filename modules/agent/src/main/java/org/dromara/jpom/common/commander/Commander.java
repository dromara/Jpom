/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander;

import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
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
            log.debug(I18nMessageUtil.get("i18n.current_system_is_linux.e377"));
        } else if (osInfo.isWindows()) {
            // Windows系统
            log.debug(I18nMessageUtil.get("i18n.current_system_is_windows.91d1"));
        } else if (osInfo.isMac()) {
            log.debug(I18nMessageUtil.get("i18n.current_system_is_mac.0139"));
        } else {
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.unsupported_item.bcf4") + osInfo.getName());
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
