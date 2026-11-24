/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * 远程下载白名单匹配测试。
 * <p>
 * 原实现只做字符串前缀匹配：白名单 {@code http://allowed.example} 下，
 * {@code http://allowed.example.evil/}（子域拼接）与 {@code http://allowed.example@evil/}（userinfo）
 * 都能通过校验却请求到攻击者主机（SSRF）。
 *
 * @author Jpom
 * @since 2.11.14.6
 */
public class ServerWhitelistTest {

    @Test
    public void testSameHostPathAllowed() {
        Assert.assertTrue(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example/file.zip", "http://allowed.example"));
        Assert.assertTrue(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example", "http://allowed.example"));
        Assert.assertTrue(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example/a/b?c=1", "http://allowed.example"));
        // 白名单写到目录边界时，前缀命中即可
        Assert.assertTrue(ServerWhitelist.matchRemoteDownloadHost("https://mirror.example.com/dist/x.tgz", "https://mirror.example.com/dist/"));
    }

    @Test
    public void testSuffixAndUserinfoBypassRejected() {
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example.evil/file.zip", "http://allowed.example"));
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example@evil.example/file.zip", "http://allowed.example"));
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example:8080/file.zip", "http://allowed.example"));
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example-other/file.zip", "http://allowed.example"));
    }

    @Test
    public void testSchemeAndHostMustMatch() {
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("ftp://allowed.example/file.zip", "http://allowed.example"));
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("http://other.example/file.zip", "http://allowed.example"));
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("http://allowed.example/file.zip", ""));
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("", "http://allowed.example"));
    }

    @Test
    public void testPortInWhitelist() {
        Assert.assertTrue(ServerWhitelist.matchRemoteDownloadHost("http://host.example:8080/file.zip", "http://host.example:8080"));
        Assert.assertFalse(ServerWhitelist.matchRemoteDownloadHost("http://host.example:8081/file.zip", "http://host.example:8080"));
    }
}
