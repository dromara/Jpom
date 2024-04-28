/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author bwcx_jzy
 * @since 2024/4/24
 */
@Data
@ConfigurationProperties("jpom.monitor")
public class MonitorConfig {

    private NetworkConfig network;

    @Data
    @ConfigurationProperties("jpom.monitor.network")
    public static class NetworkConfig {
        /**
         * 忽略的网卡,多个使用逗号分隔
         */
        private String statExcludeNames;
        /**
         * 仅统计的网卡
         * ,多个使用逗号分隔
         */
        private String statContainsOnlyNames;
    }
}
