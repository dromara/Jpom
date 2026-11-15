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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * {@link RemoteVersion#tarGzUrlOf(String)} 在线升级下载地址推导测试。
 *
 * <p>过渡期（直到 2.12.0）下载中心 release-versions.json 仍是 .zip，同时上传 tar.gz：
 * 新版本先尝试同目录同名的 tar.gz，失败回退 zip；2.12.0 后 JSON 直接给 tar.gz。
 * 这里覆盖 .zip 后缀替换、?token= 查询串保留、已是 tar.gz/.tgz、非 zip 地址等场景。
 *
 * @author bwcx_jzy
 */
public class RemoteVersionUrlTest {

    private static final String BASE = "https://d2.jpom.download/release/2.11.13.2/";

    @Test
    public void testZipSuffixReplacedWithTarGz() {
        assertEquals(BASE + "server-2.11.13.2-release.tar.gz",
            RemoteVersion.tarGzUrlOf(BASE + "server-2.11.13.2-release.zip"));
        assertEquals(BASE + "agent-2.11.13.2-release.tar.gz",
            RemoteVersion.tarGzUrlOf(BASE + "agent-2.11.13.2-release.zip"));
    }

    @Test
    public void testQueryStringPreserved() {
        assertEquals(BASE + "server-2.11.13.2-release.tar.gz?token=abc123",
            RemoteVersion.tarGzUrlOf(BASE + "server-2.11.13.2-release.zip?token=abc123"));
        assertEquals(BASE + "server-2.11.13.2-release.tar.gz?token=abc123&v=1",
            RemoteVersion.tarGzUrlOf(BASE + "server-2.11.13.2-release.zip?token=abc123&v=1"));
    }

    @Test
    public void testZipInQueryStringNotReplaced() {
        assertEquals(BASE + "server-2.11.13.2-release.tar.gz?token=download.zip",
            RemoteVersion.tarGzUrlOf(BASE + "server-2.11.13.2-release.zip?token=download.zip"));
    }

    @Test
    public void testUpperCaseZipSuffix() {
        assertEquals(BASE + "server-2.11.13.2-release.tar.gz?token=x",
            RemoteVersion.tarGzUrlOf(BASE + "server-2.11.13.2-release.ZIP?token=x"));
    }

    @Test
    public void testAlreadyTarGzKept() {
        String tarGzUrl = BASE + "server-2.11.13.2-release.tar.gz";
        assertEquals(tarGzUrl, RemoteVersion.tarGzUrlOf(tarGzUrl));
        String tgzUrl = BASE + "server-2.11.13.2-release.tgz?token=a";
        assertEquals(tgzUrl, RemoteVersion.tarGzUrlOf(tgzUrl));
    }

    @Test
    public void testNonZipUrlKept() {
        String jarUrl = BASE + "server-2.11.13.2-release.jar";
        assertEquals(jarUrl, RemoteVersion.tarGzUrlOf(jarUrl));
        String sha1Url = BASE + "server-2.11.13.2-release.zip.sha1";
        assertEquals(sha1Url, RemoteVersion.tarGzUrlOf(sha1Url));
        String dirUrl = BASE;
        assertEquals(dirUrl, RemoteVersion.tarGzUrlOf(dirUrl));
    }

    @Test
    public void testBlankUrl() {
        assertNull(RemoteVersion.tarGzUrlOf(null));
        assertEquals("", RemoteVersion.tarGzUrlOf(""));
    }
}
