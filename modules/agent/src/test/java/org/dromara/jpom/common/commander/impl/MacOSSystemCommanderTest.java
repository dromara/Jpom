/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.commander.impl;

import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class MacOSSystemCommanderTest {
    private static OsInfo osInfo;

    static {
        osInfo = SystemUtil.getOsInfo();
    }

    /**
     * 测试 MacOS 相关 top 命令
     * top -b 参数无法在 MacOS 上执行
     */
    @Test
    public void testGetAllMonitor() {
        log.info("is Mac: {}", osInfo.isMac());
        log.info("is Linux: {}", osInfo.isLinux());
        log.info("is MacOSX: {}", osInfo.isMacOsX());
        // Mac OS
        if (osInfo.isMac() || osInfo.isMacOsX()) {
            // String result = CommandUtil.execSystemCommand("top -l 1 -n 1");
            String result = "Processes: 423 total, 2 running, 421 sleeping, 2080 threads\n" +
                "2020/11/19 10:05:16\n" +
                "Load Avg: 1.15, 1.62, 1.83\n" +
                "CPU usage: 3.1% user, 11.5% sys, 85.92% idle\n" +
                "SharedLibs: 279M resident, 44M data, 27M linkedit.\n" +
                "MemRegions: 118945 total, 1481M resident, 133M private, 987M shared.\n" +
                "PhysMem: 8036M used (2072M wired), 154M unused.\n" +
                "VM: 2326G vsize, 2308M framework vsize, 11940793(0) swapins, 12858959(0) swapouts.\n" +
                "Networks: packets: 4774826/5179M in, 4179746/2989M out.\n" +
                "Disks: 5626236/140G read, 1788986/89G written.\n" +
                "\n" +
                "PID    COMMAND %CPU TIME     #TH #WQ #PORTS MEM   PURG CMPRS PGRP  PPID  STATE   BOOSTS %CPU_ME %CPU_OTHRS UID FAULTS COW MSGSENT MSGRECV SYSBSD SYSMACH CSW PAGEINS IDLEW POWER INSTRS CYCLES USER #MREGS RPRVT VPRVT VSIZE KPRVT KSHRD\n" +
                "35069  top     0.0  00:00.24 1/1 0   14     3428K 0B   0B    35069 28803 running *0[1]  0.00000 0.00000    0   1603   91  269374  134686  1834   138100  28  0       0     0.0   0      0      root N/A    N/A   N/A   N/A   N/A   N/A";

            log.info(result);

            log.info("-----------------------------");

//            MacOsSystemCommander macOSSystemCommander = new MacOsSystemCommander();
//            macOSSystemCommander.getAllMonitor();
        }
    }
}
