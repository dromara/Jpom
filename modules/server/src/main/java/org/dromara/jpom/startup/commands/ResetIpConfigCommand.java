/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.startup.commands;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.SystemIpConfigModel;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.startup.StartupCommand;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 重置IP配置命令
 *
 * @author bwcx_jzy
 * @since 2024/12/08
 */
@Slf4j
@Component
public class ResetIpConfigCommand implements StartupCommand {
    @Override
    public String getCommandFlag() {
        return "--rest:ip_config";
    }

    @Override
    public void execute(ApplicationContext applicationContext) {
        SystemParametersServer parametersServer = applicationContext.getBean(SystemParametersServer.class);
        parametersServer.delByKey(SystemIpConfigModel.ID);
        log.info(I18nMessageUtil.get("i18n.clear_ip_whitelist_config_success.8cf6"));
    }
}
