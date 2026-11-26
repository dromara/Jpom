/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import cn.hutool.http.GlobalHeaders;
import cn.keepbx.jpom.Type;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * {@link JpomUserAgent} 行为回归：Java 与 Go 插件端出站 User-Agent 必须是同一份契约。
 * <p>向量文件 {@code jpom-parent/contract/user-agent.tsv} 由两端测试共用，任何一端改模板
 * 都会让另一端（及本用例）失败，避免两端各自漂移。</p>
 */
public class JpomUserAgentTest {

    /**
     * 契约向量（maven 测试工作目录为模块根 modules/common）
     */
    private static final File CONTRACT = new File("../../contract/user-agent.tsv");

    @Test
    public void testContractVector() throws IOException {
        Assert.assertTrue("契约向量缺失：" + CONTRACT.getAbsolutePath(), CONTRACT.isFile());
        List<String> lines = Files.readAllLines(CONTRACT.toPath(), StandardCharsets.UTF_8);
        int checked = 0;
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            Assert.assertEquals("契约列数应为 7（type/version/osName/osVersion/arch/runtime/expect）：" + line, 7, parts.length);
            String actual = JpomUserAgent.of(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
            Assert.assertEquals("UA 与契约向量不一致：" + line, parts[6], actual);
            checked++;
        }
        Assert.assertTrue("契约向量不得为空", checked >= 5);
    }

    @Test
    public void testTypeNameFromEnum() {
        String ua = JpomUserAgent.of(Type.Agent.name(), "2.11.15.3", "Linux", "5.15.0-91-generic", "amd64", "1.8.0_512");
        Assert.assertTrue(ua, ua.endsWith(" Safari/537.36 Jpom Agent/2.11.15.3"));
        String serverUa = JpomUserAgent.of(Type.Server.name(), "2.11.15", "Linux", "5.15.0-91-generic", "amd64", "1.8.0_512");
        Assert.assertTrue(serverUa, serverUa.endsWith(" Safari/537.36 Jpom Server/2.11.15"));
    }

    @Test
    public void testApplyGlobalWritesHutoolHeaders() {
        JpomUserAgent.applyGlobal(Type.Agent, "2.11.15.3");
        List<String> values = GlobalHeaders.INSTANCE.headers().get(JpomUserAgent.HEADER_NAME);
        Assert.assertNotNull("hutool 全局头未写入 User-Agent", values);
        Assert.assertFalse(values.isEmpty());
        Assert.assertTrue(values.get(0), values.get(0).startsWith("Mozilla/5.0 ("));
        Assert.assertTrue(values.get(0), values.get(0).endsWith(" Jpom Agent/2.11.15.3"));
    }
}
