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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 运行平台归一化回归测试。
 * <p>Java 插件端新增 manifest.platform/arch 与服务端按平台选择 Go 升级包共用同一套口径，
 * 一旦口径漂移，同一台机器在「在线升级」页会显示成两个不同平台。</p>
 *
 * @author jpom
 * @since 2026/11/20
 */
public class PlatformUtilTest {

    @Test
    public void testNormalizePlatform() {
        Assertions.assertEquals("linux/amd64", PlatformUtil.normalizePlatform("Linux/AMD64"));
        Assertions.assertEquals("linux/amd64", PlatformUtil.normalizePlatform(" linux / x86_64 "));
        Assertions.assertEquals("linux/arm64", PlatformUtil.normalizePlatform("linux/aarch64"));
        Assertions.assertEquals("darwin/arm64", PlatformUtil.normalizePlatform("Mac OS X/arm64"));
        Assertions.assertEquals("windows/amd64", PlatformUtil.normalizePlatform("Windows/x64"));
        Assertions.assertNull(PlatformUtil.normalizePlatform("linux"));
        Assertions.assertNull(PlatformUtil.normalizePlatform("/amd64"));
        Assertions.assertNull(PlatformUtil.normalizePlatform(""));
    }

    @Test
    public void testOfUsesSameAliases() {
        Assertions.assertEquals("linux/amd64", PlatformUtil.of("Linux", "amd64"));
        Assertions.assertEquals("windows/amd64", PlatformUtil.of("Windows", "x86_64"));
        // osName 带版本号属于旧摘要口径：不猜平台，交由调用方用系统判定布尔值（Java 插件端 buildPlatform）
        Assertions.assertNull(PlatformUtil.of("Windows 10", "x86_64"));
        Assertions.assertEquals("darwin/arm64", PlatformUtil.of("Mac OS X", "aarch64"));
        Assertions.assertNull(PlatformUtil.of(null, "amd64"));
        Assertions.assertNull(PlatformUtil.of("Linux", null));
    }
}
