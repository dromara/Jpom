/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.controller;

import com.jcraft.jsch.ChannelSftp;
import org.dromara.jpom.TestI18nSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.InputStream;

/**
 * SSH 文件管理 new_file_folder 命令注入（CVE 报告，2026-11-14）回归测试。
 * <p>
 * 背景：`name` 曾被直接拼接进远程 `touch`/`mkdir` shell 命令，`name=cs123;id` 即可在目标主机执行任意命令。
 * 修复后创建文件/文件夹都走 SFTP API，不再拼接远程 shell；这里锁定名称校验行为与“空文件必须由 SFTP put 创建”。
 *
 * @author jpom
 * @since 2026/11/14
 */
public class BaseSshFileControllerNewFileFolderTest {

    @BeforeAll
    public static void initI18n() {
        TestI18nSupport.init();
    }

    @Test
    public void testRejectPathSeparator() {
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("a/b"));
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("/etc/passwd"));
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("a/"));
        // Hutool FileUtil.normalize 同时把 \\ 当作分隔符，Windows 部署的 Server 也不能放过
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("a\\b"));
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("..\\.."));
    }

    @Test
    public void testRejectDirectoryTraversal() {
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("."));
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName(".."));
    }

    @Test
    public void testRejectControlCharacters() {
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("a\rb"));
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("a\nb"));
        Assertions.assertThrows(IllegalStateException.class, () -> BaseSshFileController.checkNewFileName("a\0b"));
    }

    @Test
    public void testShellMetacharactersArePlainFileNameChars() {
        // 创建动作已改为 SFTP 调用，; | & $() 反引号 只是普通文件名字符，不应再被当成命令执行
        Assertions.assertDoesNotThrow(() -> BaseSshFileController.checkNewFileName("cs123;id"));
        Assertions.assertDoesNotThrow(() -> BaseSshFileController.checkNewFileName("a|b&c$(id)`id`.txt"));
        Assertions.assertDoesNotThrow(() -> BaseSshFileController.checkNewFileName("正常 文件名.txt"));
        Assertions.assertDoesNotThrow(() -> BaseSshFileController.checkNewFileName(".hidden"));
        Assertions.assertDoesNotThrow(() -> BaseSshFileController.checkNewFileName("..."));
    }

    @Test
    public void testCreateEmptyFileUsesSftpPut() throws Exception {
        ChannelSftp client = Mockito.mock(ChannelSftp.class);
        BaseSshFileController.createEmptyFile(client, "/home/demo/cs123;id");

        ArgumentCaptor<InputStream> inputCaptor = ArgumentCaptor.forClass(InputStream.class);
        Mockito.verify(client, Mockito.times(1)).put(inputCaptor.capture(), Mockito.eq("/home/demo/cs123;id"));
        // 空文件：不写入任何内容，也不经过远程 shell
        Assertions.assertEquals(0, inputCaptor.getValue().available());
    }
}
