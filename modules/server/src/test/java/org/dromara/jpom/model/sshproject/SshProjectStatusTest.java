/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.sshproject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * status 脚本输出协议解析
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
public class SshProjectStatusTest {

    @Test
    public void parseRunningWithPidAndPort() {
        SshProjectStatus status = SshProjectStatus.parse("some line\nrunning:1234:8080\n");
        Assertions.assertTrue(status.isRunning());
        Assertions.assertEquals(1234, status.getPid());
        Assertions.assertEquals("8080", status.getPorts());
    }

    @Test
    public void parseRunningWithoutPid() {
        SshProjectStatus status = SshProjectStatus.parse("running:0");
        Assertions.assertTrue(status.isRunning());
        Assertions.assertEquals(0, status.getPid());
        Assertions.assertNull(status.getPorts());
    }

    @Test
    public void parseStopped() {
        Assertions.assertFalse(SshProjectStatus.parse("stopped").isRunning());
        Assertions.assertFalse(SshProjectStatus.parse("").isRunning());
        Assertions.assertFalse(SshProjectStatus.parse("任意错误信息").isRunning());
    }

    @Test
    public void parseTakesLastLine() {
        SshProjectStatus status = SshProjectStatus.parse("running:1:80\nstopped\n");
        Assertions.assertFalse(status.isRunning());
    }
}
