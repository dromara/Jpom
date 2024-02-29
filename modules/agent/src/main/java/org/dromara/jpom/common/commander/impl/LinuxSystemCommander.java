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

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.commander.AbstractSystemCommander;
import org.dromara.jpom.common.commander.Commander;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Slf4j
@Conditional(Commander.Linux.class)
@Service
@Primary
public class LinuxSystemCommander extends AbstractSystemCommander {


    @Override
    public String emptyLogFile(File file) {
        return CommandUtil.execSystemCommand("cp /dev/null " + file.getAbsolutePath());
    }


//    @Override
//    public boolean getServiceStatus(String serviceName) {
//        if (StrUtil.startWith(serviceName, StrUtil.SLASH)) {
//            String ps = getPs(serviceName);
//            return StrUtil.isNotEmpty(ps);
//        }
//        String format = StrUtil.format("service {} status", serviceName);
//        String result = CommandUtil.execSystemCommand(format);
//        return StrUtil.containsIgnoreCase(result, "RUNNING");
//    }
//
//    @Override
//    public String startService(String serviceName) {
//        if (StrUtil.startWith(serviceName, StrUtil.SLASH)) {
//            try {
//                CommandUtil.asyncExeLocalCommand(FileUtil.file(SystemUtil.getUserInfo().getHomeDir()), serviceName);
//                return "ok";
//            } catch (Exception e) {
//                log.error("执行异常", e);
//                return "执行异常：" + e.getMessage();
//            }
//        }
//        String format = StrUtil.format("service {} start", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }
//
//    @Override
//    public String stopService(String serviceName) {
//        if (StrUtil.startWith(serviceName, StrUtil.SLASH)) {
//            String ps = getPs(serviceName);
//            List<String> list = StrUtil.splitTrim(ps, StrUtil.LF);
//            if (list == null || list.isEmpty()) {
//                return "stop";
//            }
//            String s = list.get(0);
//            list = StrUtil.splitTrim(s, StrUtil.SPACE);
//            if (list == null || list.size() < 2) {
//                return "stop";
//            }
//            File file = new File(SystemUtil.getUserInfo().getHomeDir());
//            int pid = Convert.toInt(list.get(1), 0);
//            if (pid <= 0) {
//                return "error stop";
//            }
//            return kill(file, pid);
//        }
//        String format = StrUtil.format("service {} stop", serviceName);
//        return CommandUtil.execSystemCommand(format);
//    }

    @Override
    public String buildKill(int pid) {
        return String.format("kill  %s", pid);
    }

    private String getPs(String serviceName) {
        String ps = StrUtil.format("ps -ef | grep -v 'grep' | egrep {}", serviceName);
        return CommandUtil.execSystemCommand(ps);
    }
}
