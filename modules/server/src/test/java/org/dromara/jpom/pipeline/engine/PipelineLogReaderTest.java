/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 运行日志窗口读取（长日志不再整文件进内存）
 *
 * @author jpom
 * @since 2.11.16
 */
public class PipelineLogReaderTest {

    private File logFile(int lines) throws Exception {
        File file = File.createTempFile("pipeline-log-", ".log");
        file.deleteOnExit();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append("line-").append(i).append('\n');
        }
        Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    @Test
    public void readsFirstPageAndReportsMore() throws Exception {
        File file = logFile(2500);
        PipelineLogReader.Window window = PipelineLogReader.read(file, 1, 1000);
        List<String> lines = window.getLines();
        Assertions.assertEquals(1000, lines.size());
        Assertions.assertEquals("line-1", lines.get(0));
        Assertions.assertEquals("line-1000", lines.get(999));
        Assertions.assertTrue(window.isHasMore(), "还有第 2 页");
    }

    @Test
    public void readsTailPageWithoutMore() throws Exception {
        File file = logFile(2500);
        PipelineLogReader.Window window = PipelineLogReader.read(file, 2001, 1000);
        Assertions.assertEquals(500, window.getLines().size());
        Assertions.assertEquals("line-2500", window.getLines().get(499));
        Assertions.assertFalse(window.isHasMore(), "已到末尾");
        // 起点 = 当前行 + 已读行数，前端据此翻页
        Assertions.assertEquals(2501, 2001 + window.getLines().size());
    }

    @Test
    public void startBeyondEndReturnsEmptyWindow() throws Exception {
        File file = logFile(10);
        PipelineLogReader.Window window = PipelineLogReader.read(file, 100, 1000);
        Assertions.assertTrue(window.getLines().isEmpty());
        Assertions.assertFalse(window.isHasMore());
    }

    @Test
    public void missingFileReturnsEmptyWindow() throws Exception {
        File file = new File(System.getProperty("java.io.tmpdir"), "pipeline-log-not-exists-" + System.nanoTime() + ".log");
        PipelineLogReader.Window window = PipelineLogReader.read(file, 1, 100);
        Assertions.assertTrue(window.getLines().isEmpty());
        Assertions.assertFalse(window.isHasMore());
    }
}
