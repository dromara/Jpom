/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.io.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * {@link LogRecorder#appendRaw(String)} 行为回归。
 * <p>
 * 远程构建节点的日志行必须原样落盘：行内的 {} / %s / 反斜杠都不能被模板解析吃掉，
 * 否则构建日志会被篡改（排查时看到的内容与实际输出不一致）。
 */
public class LogRecorderRawTest {

    @Test
    public void testAppendRawKeepsTemplateChars() {
        File file = FileUtil.file(FileUtil.getTmpDir(), "jpom-log-recorder-raw-" + System.nanoTime() + ".log");
        try {
            LogRecorder recorder = LogRecorder.builder().file(file).build();
            recorder.appendRaw("mvn -Dparam={} -Dother=%s 中文 {}");
            recorder.appendRaw("[INFO] BUILD SUCCESS");
            recorder.system("system line {}", "value");
            recorder.close();
            List<String> lines = FileUtil.readLines(file, StandardCharsets.UTF_8);
            Assert.assertEquals("mvn -Dparam={} -Dother=%s 中文 {}", lines.get(0));
            Assert.assertEquals("[INFO] BUILD SUCCESS", lines.get(1));
            Assert.assertEquals("[SYSTEM-INFO] system line value", lines.get(2));
        } finally {
            FileUtil.del(file);
        }
    }
}
