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

import org.junit.Assert;
import org.junit.Test;

/**
 * 升级包下载失败信息脱敏与可读前缀回归。
 *
 * <p>背景：失败时 {@code HttpUtil} 的异常信息可能带完整下载地址（含 {@code ?token=} 授权码），
 * 旧实现把原始信息直接回给浏览器（403 分支）或用 {@code sneakyThrow} 落到「服务异常」兜底（其他分支）。
 * 现在统一脱敏并以「下载失败」前缀返回，便于用户按提示排查。</p>
 *
 * @author jpom
 * @since 2.11.14.7
 */
public class RemoteVersionDownloadMessageTest {

    private static final String BASE = "https://d2.jpom.download/release/2.11.14/server-2.11.14-release.zip";

    @Test
    public void tokenIsRedacted() {
        Assert.assertEquals(BASE + "?token=***",
            RemoteVersion.sanitizeDownloadMessage(BASE + "?token=abc123"));
    }

    @Test
    public void tokenAmongParamsIsRedacted() {
        String message = "Server response error with status code: [404] for url " + BASE + "?v=1&access_token=secret&x=2";
        String sanitized = RemoteVersion.sanitizeDownloadMessage(message);
        Assert.assertFalse("授权码不得回显：" + sanitized, sanitized.contains("secret"));
        Assert.assertTrue(sanitized, sanitized.contains("access_token=***"));
        Assert.assertTrue(sanitized, sanitized.contains("&x=2"));
    }

    @Test
    public void messageWithoutTokenIsKept() {
        String message = "ConnectException: Connection refused (Connection refused)";
        Assert.assertEquals(message, RemoteVersion.sanitizeDownloadMessage(message));
        Assert.assertEquals("", RemoteVersion.sanitizeDownloadMessage(null));
        Assert.assertEquals("", RemoteVersion.sanitizeDownloadMessage("  "));
    }
}
