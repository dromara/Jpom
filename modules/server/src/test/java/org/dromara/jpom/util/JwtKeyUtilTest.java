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
import org.dromara.jpom.configuration.UserConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 JWT 密钥初始化：未配置时生成并持久化，配置时优先使用配置值，空白值不会回退到公开固定密钥。
 *
 * @author jpom
 */
public class JwtKeyUtilTest {

    @TempDir
    File tempDir;

    @Test
    public void testConfiguredKeyIsUsed() {
        UserConfig userConfig = new UserConfig();
        userConfig.setTokenJwtKey(" 0123456789abcdef0123456789abcdef ");
        JwtKeyUtil.ensureKey(userConfig, tempDir.getAbsolutePath());
        assertEquals("0123456789abcdef0123456789abcdef", userConfig.getTokenJwtKey());
        // 显式配置时不应再生成密钥文件
        assertFalse(FileUtil.exist(FileUtil.file(tempDir, JwtKeyUtil.KEY_FILE_NAME)));
    }

    @Test
    public void testBlankKeyGeneratedAndPersisted() {
        UserConfig userConfig = new UserConfig();
        userConfig.setTokenJwtKey("   ");
        JwtKeyUtil.ensureKey(userConfig, tempDir.getAbsolutePath());

        String key = userConfig.getTokenJwtKey();
        assertTrue(key != null && key.length() >= 32, "自动生成的 JWT 密钥长度应不小于 32");
        assertNotEquals("KZQfFBJTW2v6obS1", key, "不得回退到源码中的固定密钥");

        File keyFile = FileUtil.file(tempDir, JwtKeyUtil.KEY_FILE_NAME);
        assertTrue(FileUtil.exist(keyFile), "自动生成的密钥应持久化到数据目录");
        assertEquals(key, FileUtil.readString(keyFile, StandardCharsets.UTF_8).trim());

        // 再次启动应复用同一密钥
        UserConfig next = new UserConfig();
        JwtKeyUtil.ensureKey(next, tempDir.getAbsolutePath());
        assertEquals(key, next.getTokenJwtKey());
    }

    @Test
    public void testEmptyKeyFileRegenerated() {
        File keyFile = FileUtil.file(tempDir, JwtKeyUtil.KEY_FILE_NAME);
        FileUtil.writeString("   \n", keyFile, StandardCharsets.UTF_8);

        UserConfig userConfig = new UserConfig();
        JwtKeyUtil.ensureKey(userConfig, tempDir.getAbsolutePath());
        String key = userConfig.getTokenJwtKey();
        assertTrue(key != null && !key.trim().isEmpty());
        assertEquals(key, FileUtil.readString(keyFile, StandardCharsets.UTF_8).trim());
    }

    @Test
    public void testTokenJwtKeyByteRequiresInitializedKey() {
        UserConfig userConfig = new UserConfig();
        assertThrows(IllegalStateException.class, userConfig::getTokenJwtKeyByte);

        userConfig.setTokenJwtKey("密钥-UTF8");
        assertEquals("密钥-UTF8", new String(userConfig.getTokenJwtKeyByte(), StandardCharsets.UTF_8));
    }
}
