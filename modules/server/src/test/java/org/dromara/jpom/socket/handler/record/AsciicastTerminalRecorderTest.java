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

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * asciicast v2 录制器
 *
 * @author bwcx_jzy
 * @since 2026/11/26
 */
public class AsciicastTerminalRecorderTest {

    private TerminalRecordContext context() {
        TerminalRecordContext context = new TerminalRecordContext();
        context.setSessionId("test-session-1");
        context.setWorkspaceId("DEFAULT");
        context.setUserId("user1");
        context.setMachineSshId("m1");
        context.setMachineSshName("asset-1");
        context.setCols(120);
        context.setRows(40);
        return context;
    }

    /**
     * header 与事件流格式符合 asciicast v2
     */
    @Test
    public void testWriteAsciicast() throws Exception {
        File rootDir = Files.createTempDirectory("jpom-cast").toFile();
        TerminalRecorder recorder = AsciicastTerminalRecorder.startAt(rootDir, context(), StandardCharsets.UTF_8, null);
        recorder.output("\u001b[32mhello\u001b[0m\r\n".getBytes(StandardCharsets.UTF_8));
        recorder.input("ls -l\r".getBytes(StandardCharsets.UTF_8));
        recorder.resize(100, 30);
        JSONObject marker = new JSONObject();
        marker.put("seq", 1);
        marker.put("exit", 0);
        marker.put("cmd", "ls -l");
        marker.put("source", "OSC133");
        recorder.marker(marker.toJSONString());
        AsciicastTerminalRecorder castRecorder = (AsciicastTerminalRecorder) recorder;
        File castFile = castRecorder.getFile();
        Assertions.assertTrue(FileUtil.exist(castFile));
        recorder.close();
        // 幂等：重复 close 不抛异常
        recorder.close();
        List<String> lines = FileUtil.readUtf8Lines(castFile);
        Assertions.assertTrue(lines.size() >= 5);
        JSONObject header = JSON.parseObject(lines.get(0));
        Assertions.assertEquals(Integer.valueOf(2), header.getInteger("version"));
        Assertions.assertEquals(Integer.valueOf(120), header.getInteger("width"));
        Assertions.assertEquals(Integer.valueOf(40), header.getInteger("height"));
        JSONArray output = JSON.parseArray(lines.get(1));
        Assertions.assertEquals("o", output.getString(1));
        Assertions.assertTrue(output.getString(2).contains("hello"));
        JSONArray input = JSON.parseArray(lines.get(2));
        Assertions.assertEquals("i", input.getString(1));
        JSONArray resize = JSON.parseArray(lines.get(3));
        Assertions.assertEquals("r", resize.getString(1));
        Assertions.assertEquals("100x30", resize.getString(2));
        JSONArray markerLine = JSON.parseArray(lines.get(4));
        Assertions.assertEquals("m", markerLine.getString(1));
        JSONObject markerData = JSON.parseObject(markerLine.getString(2));
        Assertions.assertEquals("ls -l", markerData.getString("cmd"));
        Assertions.assertEquals(1, castRecorder.getCommandCount());
        // 时间戳单调递增
        double pre = -1;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).startsWith("[")) {
                JSONArray array = JSON.parseArray(lines.get(i));
                double time = array.getDoubleValue(0);
                Assertions.assertTrue(time >= pre);
                pre = time;
            }
        }
    }

    /**
     * 关闭后继续写入不再产生内容（不能抛异常）
     */
    @Test
    public void testWriteAfterClose() throws Exception {
        File rootDir = Files.createTempDirectory("jpom-cast2").toFile();
        TerminalRecorder recorder = AsciicastTerminalRecorder.startAt(rootDir, context(), StandardCharsets.UTF_8, null);
        recorder.close();
        recorder.output("late".getBytes(StandardCharsets.UTF_8));
        recorder.marker("{}");
        AsciicastTerminalRecorder castRecorder = (AsciicastTerminalRecorder) recorder;
        List<String> lines = FileUtil.readUtf8Lines(castRecorder.getFile());
        Assertions.assertEquals(1, lines.size());
    }

    /**
     * 非法 UTF-8 字节不会写出非法 JSON（按替换字符记录）
     */
    @Test
    public void testInvalidBytes() throws Exception {
        File rootDir = Files.createTempDirectory("jpom-cast3").toFile();
        TerminalRecorder recorder = AsciicastTerminalRecorder.startAt(rootDir, context(), StandardCharsets.UTF_8, null);
        recorder.output(new byte[]{(byte) 0xff, (byte) 0xfe, 'o', 'k'});
        recorder.close();
        AsciicastTerminalRecorder castRecorder = (AsciicastTerminalRecorder) recorder;
        List<String> lines = FileUtil.readUtf8Lines(castRecorder.getFile());
        Assertions.assertEquals(2, lines.size());
        // 第二行必须是合法 JSON 数组
        JSONArray array = JSON.parseArray(lines.get(1));
        Assertions.assertEquals("o", array.getString(1));
    }
}
