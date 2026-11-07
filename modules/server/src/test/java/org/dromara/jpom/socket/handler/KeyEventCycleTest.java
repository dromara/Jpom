/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 控制台按键事件处理测试
 *
 * @author bwcx_jzy
 * @since 2026/11/2
 */
public class KeyEventCycleTest {

    private KeyEventCycle createCycle() {
        KeyEventCycle cycle = new KeyEventCycle();
        cycle.setCharset(StandardCharsets.UTF_8);
        return cycle;
    }

    /**
     * 空命令行按下退格键、Tab 键时不能抛出索引越界异常，命令缓冲不能被破坏
     */
    @Test
    public void testBackspaceAndTabOnEmptyLine() {
        KeyEventCycle cycle = createCycle();
        List<String> commands = new ArrayList<>();
        // 空命令行按下退格键
        cycle.read(commands::add, new byte[]{127});
        // 空命令行按下 Tab 键，服务端回显终端响铃
        cycle.read(commands::add, new byte[]{9});
        cycle.receive(new byte[]{7});
        // Tab 回车后服务端回显退格控制字符
        cycle.read(commands::add, new byte[]{9});
        cycle.receive(new byte[]{8});
        // 继续输入命令并回车，命令缓冲应该正常
        cycle.read(commands::add, "ls".getBytes(StandardCharsets.UTF_8));
        cycle.read(commands::add, new byte[]{13});
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals("ls", commands.get(0));
    }

    /**
     * 光标在行首时服务端回显退格控制字符，不能删除字符也不能抛出索引越界异常
     */
    @Test
    public void testBackspaceAtLineStart() {
        KeyEventCycle cycle = createCycle();
        List<String> commands = new ArrayList<>();
        cycle.read(commands::add, "abc".getBytes(StandardCharsets.UTF_8));
        // Home 键将光标移动到行首
        cycle.read(commands::add, new byte[]{27, 91, 72});
        // Tab 后服务端回显退格控制字符
        cycle.read(commands::add, new byte[]{9});
        cycle.receive(new byte[]{8});
        // 回车后命令应该保持完整
        cycle.read(commands::add, new byte[]{13});
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals("abc", commands.get(0));
    }

    /**
     * Tab 补全回显的内容在空命令行时从行首开始插入
     */
    @Test
    public void testTabEchoOnEmptyLine() {
        KeyEventCycle cycle = createCycle();
        List<String> commands = new ArrayList<>();
        // 空命令行按下 Tab 键
        cycle.read(commands::add, new byte[]{9});
        cycle.receive("ls".getBytes(StandardCharsets.UTF_8));
        // 补全回显后继续输入，光标应该在命令末尾
        cycle.read(commands::add, " -l".getBytes(StandardCharsets.UTF_8));
        // 回车后命令应该为 Tab 补全回显的内容
        cycle.read(commands::add, new byte[]{13});
        Assert.assertEquals(1, commands.size());
        Assert.assertEquals("ls -l", commands.get(0));
    }
}
