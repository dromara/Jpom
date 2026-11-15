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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验客户端可控文件名解析：必须限制在指定目录内，禁止 ../ 越界与符号链接逃逸。
 */
public class FileUtilsResolveSubFileTest {

    @TempDir
    Path tempDir;

    @Test
    public void testResolveInsideBase() throws Exception {
        Path base = Files.createDirectory(tempDir.resolve("logs"));
        Files.write(base.resolve("server.log"), "hello".getBytes());
        File file = FileUtils.resolveSubFile(base.toFile(), "server.log");
        assertNotNull(file);
        assertTrue(file.getCanonicalPath().startsWith(base.toFile().getCanonicalPath() + File.separator));
        // 兼容前端日志列表返回的 "/server.log" 形式
        File slashFile = FileUtils.resolveSubFile(base.toFile(), "/server.log");
        assertNotNull(slashFile);
        assertTrue(slashFile.getCanonicalPath().startsWith(base.toFile().getCanonicalPath() + File.separator));
    }

    @Test
    public void testRejectTraversal() throws Exception {
        Path base = Files.createDirectory(tempDir.resolve("logs2"));
        Files.write(tempDir.resolve("outside.txt"), "outside".getBytes());
        String basePath = base.toFile().getCanonicalPath() + File.separator;
        // 任何返回结果都必须落在 base 内；../ 必须被拒绝
        assertNull(FileUtils.resolveSubFile(base.toFile(), "../outside.txt"));
        assertNull(FileUtils.resolveSubFile(base.toFile(), "a/../../outside.txt"));
        String[] suspicious = new String[]{"..\\outside.txt", "..\\..\\outside.txt"};
        for (String value : suspicious) {
            File file = FileUtils.resolveSubFile(base.toFile(), value);
            if (file != null) {
                assertTrue(file.getCanonicalPath().startsWith(basePath));
            }
        }
        assertNull(FileUtils.resolveSubFile(base.toFile(), ""));
        assertNull(FileUtils.resolveSubFile(base.toFile(), null));
    }

    @Test
    public void testRejectSymlinkEscape() throws Exception {
        Path base = Files.createDirectory(tempDir.resolve("logs4"));
        Path outside = tempDir.resolve("symlink-outside.txt");
        Files.write(outside, "outside".getBytes());
        try {
            Files.createSymbolicLink(base.resolve("link.log"), outside);
        } catch (Exception e) {
            return; // 文件系统不支持符号链接时跳过
        }
        assertNull(FileUtils.resolveSubFile(base.toFile(), "link.log"));
    }

    @Test
    public void testRejectAbsoluteEscape() throws Exception {
        Path base = Files.createDirectory(tempDir.resolve("logs3"));
        File outside = tempDir.resolve("absolute-outside.txt").toFile();
        Files.write(outside.toPath(), "x".getBytes());
        // 绝对路径不会逃逸到系统目录之外：要么拒绝，要么仍落在 base 内
        File file = FileUtils.resolveSubFile(base.toFile(), outside.getAbsolutePath());
        if (file != null) {
            assertTrue(file.getCanonicalPath().startsWith(base.toFile().getCanonicalPath() + File.separator));
        }
    }
}
