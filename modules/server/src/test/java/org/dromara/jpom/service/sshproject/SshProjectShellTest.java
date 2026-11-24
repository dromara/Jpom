/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 脚本拼接的转义与命令组装（纯函数）
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
public class SshProjectShellTest {

    @Test
    public void quoteEscapesSingleQuote() {
        Assertions.assertEquals("'abc'", SshProjectShell.quote("abc"));
        Assertions.assertEquals("''", SshProjectShell.quote(null));
        // 单引号必须被转成结束单引号 + 转义单引号 + 重新开始单引号，不能直接闭合
        String quoted = SshProjectShell.quote("a'b");
        Assertions.assertEquals("'a'\"'\"'b'", quoted);
        Assertions.assertFalse(quoted.contains(";"), "转义结果不应引入命令分隔符");
    }

    @Test
    public void envBlockOnlyAcceptsValidNames() {
        Map<String, String> env = new LinkedHashMap<>();
        env.put("PROJECT_ID", "abc");
        env.put("BAD-NAME", "x");
        env.put("PROJECT_NAME", "demo app");
        String block = SshProjectShell.envBlock(env);
        // 不能注入 set -e：status/stop 脚本常用「测试为假就继续」写法，-e 会直接中断
        Assertions.assertFalse(block.contains("set -e"));
        Assertions.assertTrue(block.contains("export PROJECT_ID='abc'"));
        Assertions.assertTrue(block.contains("export PROJECT_NAME='demo app'"));
        Assertions.assertFalse(block.contains("BAD-NAME"));
    }

    @Test
    public void execCommandWrapsTimeout() {
        String command = SshProjectShell.execCommand("/home/u/.jpom/scripts/a.sh", "--port=8080", 30);
        Assertions.assertEquals("timeout -k 5 30 bash '/home/u/.jpom/scripts/a.sh' --port=8080", command);
        // 超时下限为 1 秒
        Assertions.assertTrue(SshProjectShell.execCommand("/a.sh", "", 0).contains("timeout -k 5 1 bash"));
    }
}
