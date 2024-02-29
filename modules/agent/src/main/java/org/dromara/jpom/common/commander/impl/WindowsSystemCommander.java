/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander.impl;

import org.dromara.jpom.common.commander.AbstractSystemCommander;
import org.dromara.jpom.common.commander.Commander;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * windows 系统查询命令
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Conditional(Commander.Windows.class)
@Service
public class WindowsSystemCommander extends AbstractSystemCommander {

    @Override
    public String emptyLogFile(File file) {
        return CommandUtil.execSystemCommand("echo  \"\" > " + file.getAbsolutePath());
    }


//    @Override
//    public boolean getServiceStatus(String serviceName) {
//        String result = CommandUtil.execSystemCommand("sc query " + serviceName);
//        return StrUtil.containsIgnoreCase(result, "RUNNING");
//    }
//
//    @Override
//    public String startService(String serviceName) {
//        String format = StrUtil.format("net start {}", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }
//
//    @Override
//    public String stopService(String serviceName) {
//        String format = StrUtil.format("net stop {}", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }

    @Override
    public String buildKill(int pid) {
        return String.format("taskkill /F /PID %s", pid);
    }
}
