/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.startup;

import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * 命令执行器
 *
 * @author bwcx_jzy
 * @since 2024/12/08
 */
@Slf4j
public class CommandExecutor {
    private final ApplicationContext applicationContext;
    private final String[] args;

    public CommandExecutor(ApplicationContext applicationContext, String[] args) {
        this.applicationContext = applicationContext;
        this.args = args;
    }

    public void execute() {
        for (StartupCommand command : applicationContext.getBeansOfType(StartupCommand.class).values()) {
            String commandFlag = command.getCommandFlag();
            if (ArrayUtil.containsIgnoreCase(args, commandFlag)) {
                try {
                    command.execute(applicationContext);
                    log.info("Successfully executed command: {}", commandFlag);
                } catch (Exception e) {
                    log.error("Failed to execute command: {}", commandFlag, e);
                }
            }
        }
    }
}
