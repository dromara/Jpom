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

import org.springframework.context.ApplicationContext;

/**
 * 启动命令接口
 *
 * @author bwcx_jzy
 * @since 2024/12/08
 */
public interface StartupCommand {
    /**
     * 获取命令标识
     *
     * @return 命令标识
     */
    String getCommandFlag();

    /**
     * 执行命令
     *
     * @param applicationContext Spring上下文
     * @throws Exception 执行异常
     */
    void execute(ApplicationContext applicationContext) throws Exception;
}
