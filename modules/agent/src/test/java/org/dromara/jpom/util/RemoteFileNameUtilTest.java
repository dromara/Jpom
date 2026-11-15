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

import org.junit.Assert;
import org.junit.Test;

/**
 * 远程下载文件名称解析测试。
 *
 * @author Jpom
 * @since 2.11.13.2
 */
public class RemoteFileNameUtilTest {

    @Test
    public void testUrlBaseName() {
        Assert.assertEquals("remote.txt", RemoteFileNameUtil.resolveFileName("http://127.0.0.1:8080/remote.txt"));
        Assert.assertEquals("remote.txt", RemoteFileNameUtil.resolveFileName("http://127.0.0.1:8080/files/remote.txt"));
        Assert.assertEquals("a.jar", RemoteFileNameUtil.resolveFileName("https://example.com/download/a.jar?version=1#frag"));
        Assert.assertEquals("b.txt", RemoteFileNameUtil.resolveFileName("http://example.com/a/b.txt"));
        // 反斜杠按路径分隔符处理（与 Go 插件端 sanitizeRemoteName 的 path.Base 行为一致）
        Assert.assertEquals("b.txt", RemoteFileNameUtil.resolveFileName("http://example.com/a\\b.txt"));
        // 以 / 结尾时取上一级名称，与 Go 的 path.Base 一致
        Assert.assertEquals("files", RemoteFileNameUtil.resolveFileName("http://example.com/files/"));
    }

    @Test
    public void testUrlDecode() {
        Assert.assertEquals("中文.txt", RemoteFileNameUtil.resolveFileName("http://example.com/%E4%B8%AD%E6%96%87.txt"));
        Assert.assertEquals("a b.txt", RemoteFileNameUtil.resolveFileName("http://example.com/a%20b.txt"));
        // 路径中的 + 不是空格
        Assert.assertEquals("a+b.txt", RemoteFileNameUtil.resolveFileName("http://example.com/a+b.txt"));
    }

    @Test
    public void testFallbackToRandomName() {
        String[] urls = {
            null,
            "",
            "   ",
            "not-a-url",
            "http://example.com/",
            // 路径穿越
            "http://example.com/%2e%2e",
            "http://example.com/%2E%2E",
            "http://example.com/a%2F..%2F..%2Fetc%2Fpasswd",
            "http://example.com/%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            // 控制字符
            "http://example.com/a%00b.txt",
            "http://example.com/a%01b.txt",
            // Windows 非法字符
            "http://example.com/a:b.txt",
            "http://example.com/a%3Fb.txt",
            "http://example.com/a*b.txt",
            // Windows 会去掉末尾的点和空格
            "http://example.com/a.txt.",
            "http://example.com/a.txt%20",
        };
        for (String url : urls) {
            String fileName = RemoteFileNameUtil.resolveFileName(url);
            Assert.assertTrue("url=" + url + " fileName=" + fileName,
                fileName.matches("[0-9a-f]{32}"));
        }
    }

    @Test
    public void testResolvedNameIsSafe() {
        String[] urls = {
            "http://example.com/a%2Fb.txt",
            "http://example.com/%5C..%5C..%5Cetc%5Cpasswd",
            "http://example.com/../a.txt",
        };
        for (String url : urls) {
            String fileName = RemoteFileNameUtil.resolveFileName(url);
            Assert.assertFalse(fileName.isEmpty());
            Assert.assertFalse("url=" + url + " fileName=" + fileName, fileName.contains("/"));
            Assert.assertFalse("url=" + url + " fileName=" + fileName, fileName.contains("\\"));
            Assert.assertFalse("url=" + url + " fileName=" + fileName, "..".equals(fileName));
        }
    }
}
