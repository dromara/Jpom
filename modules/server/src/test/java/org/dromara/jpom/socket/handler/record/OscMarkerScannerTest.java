/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler.record;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * OSC 133 / 633 命令标记解析
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
public class OscMarkerScannerTest {

    private static final String E = "\u001b]";
    private static final String BEL = "\u0007";

    /**
     * 完整序列：636;E 给出命令原文，133;D 给出退出码
     */
    @Test
    public void testFullSequence() {
        OscMarkerScanner scanner = new OscMarkerScanner();
        List<String> commands = new ArrayList<>();
        List<Integer> exits = new ArrayList<>();
        String data = E + "633;E;ls -l" + BEL + E + "133;D;0" + BEL;
        scanner.scan(data, (command, exitCode) -> {
            commands.add(command);
            exits.add(exitCode);
        });
        Assertions.assertEquals(1, commands.size());
        Assertions.assertEquals("ls -l", commands.get(0));
        Assertions.assertEquals(Integer.valueOf(0), exits.get(0));
        Assertions.assertTrue(scanner.isSeen());
    }

    /**
     * 序列被拆到多个输出包里（真实终端必然出现）仍能解析
     */
    @Test
    public void testSplitChunks() {
        OscMarkerScanner scanner = new OscMarkerScanner();
        List<String> commands = new ArrayList<>();
        String data = "\u001b]633;E;echo 中文" + BEL + "\u001b]133;D;3" + BEL;
        // 逐字节喂入，验证跨包缓冲
        for (char c : data.toCharArray()) {
            scanner.scan(String.valueOf(c), (command, exitCode) -> commands.add(command + "#" + exitCode));
        }
        Assertions.assertEquals(1, commands.size());
        Assertions.assertEquals("echo 中文#3", commands.get(0));
    }

    /**
     * ESC \\ (ST) 作为结束符同样支持
     */
    @Test
    public void testStTerminator() {
        OscMarkerScanner scanner = new OscMarkerScanner();
        List<String> commands = new ArrayList<>();
        String data = "\u001b]633;E;whoami\u001b\\\u001b]133;D;1\u001b\\";
        scanner.scan(data, (command, exitCode) -> commands.add(command + "#" + exitCode));
        Assertions.assertEquals(1, commands.size());
        Assertions.assertEquals("whoami#1", commands.get(0));
    }

    /**
     * 注入脚本自身产生的伪命令不进入命令索引
     */
    @Test
    public void testIgnoreInjectedCommand() {
        OscMarkerScanner scanner = new OscMarkerScanner();
        List<String> commands = new ArrayList<>();
        String data = E + "633;E;if [ -n \"$BASH_VERSION\" ]; then __jpom_mark(){}; fi" + BEL + E + "133;D;0" + BEL;
        scanner.scan(data, (command, exitCode) -> commands.add(String.valueOf(command)));
        // 伪命令被过滤后没有可索引的命令原文，不产生任何命令行
        Assertions.assertTrue(commands.isEmpty());
    }

    /**
     * fc/history 取到的命令行带前导制表符要 trim，空命令不产生索引
     */
    @Test
    public void testTrimAndSkipEmpty() {
        OscMarkerScanner scanner = new OscMarkerScanner();
        List<String> commands = new ArrayList<>();
        String data = E + "633;E;\t ls -l /tmp" + BEL + E + "133;D;0" + BEL
            + E + "133;D;0" + BEL;
        scanner.scan(data, (command, exitCode) -> commands.add(command));
        Assertions.assertEquals(1, commands.size());
        Assertions.assertEquals("ls -l /tmp", commands.get(0));
    }

    /**
     * 没有标记的普通输出不产生命令，也不能抛异常
     */
    @Test
    public void testPlainOutput() {
        OscMarkerScanner scanner = new OscMarkerScanner();
        List<String> commands = new ArrayList<>();
        scanner.scan("hello world\r\n\u001b[31mred\u001b[0m\r\n", (command, exitCode) -> commands.add(String.valueOf(command)));
        scanner.scan("\u001b]8;;http://example.com\u0007link\u001b]8;;\u0007", (command, exitCode) -> commands.add(String.valueOf(command)));
        Assertions.assertTrue(commands.isEmpty());
        Assertions.assertFalse(scanner.isSeen());
    }

    /**
     * 半截序列不会一直堆积（超长缓冲自动丢弃）
     */
    @Test
    public void testGarbageBuffer() {
        OscMarkerScanner scanner = new OscMarkerScanner();
        StringBuilder builder = new StringBuilder("\u001b]633;E;");
        for (int i = 0; i < 20000; i++) {
            builder.append('a');
        }
        scanner.scan(builder.toString(), (command, exitCode) -> {
        });
        // 不抛异常即可，缓冲区不会无限增长
        Assertions.assertFalse(scanner.isSeen());
    }
}
