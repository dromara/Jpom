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
import org.dromara.jpom.service.user.UserService;
import org.dromara.jpom.startup.StartupCommand;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 关闭超级管理员MFA命令
 *
 * @author bwcx_jzy
 * @since 2024/12/08
 */
@Slf4j
@Component
public class CloseSuperUserMfaCommand implements StartupCommand {
    @Override
    public String getCommandFlag() {
        return "--close:super_user_mfa";
    }

    @Override
    public void execute(ApplicationContext applicationContext) {
        UserService userService = applicationContext.getBean(UserService.class);
        String restResult = userService.closeSuperUserMfa();
        if (restResult != null) {
            log.info(restResult);
        } else {
            log.error(I18nMessageUtil.get("i18n.no_super_admin_account.538d"));
        }
    }
}
