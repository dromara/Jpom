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
import lombok.EqualsAndHashCode;
import org.dromara.jpom.socket.ServiceFileTailWatcher;
import org.dromara.jpom.system.BaseSystemConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;

/**
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("jpom.system")
public class SystemConfig extends BaseSystemConfig {

    @Override
    public void setLogCharset(Charset logCharset) {
        super.setLogCharset(logCharset);
        ServiceFileTailWatcher.setCharset(getLogCharset());
    }
}
