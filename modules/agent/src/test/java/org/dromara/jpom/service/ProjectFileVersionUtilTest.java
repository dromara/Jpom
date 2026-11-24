/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.hutool.core.io.FileUtil;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

/**
 * 版本快照目录操作测试：确保 logs/temp/临时目录不进入版本、不被清理，指纹稳定可校验。
 *
 * @author Jpom
 * @since 2.11.15
 */
public class ProjectFileVersionUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * 构造一个带 logs、temp、临时目录的项目目录
     */
    private File newProjectDir() throws Exception {
        File lib = temporaryFolder.newFolder("lib");
        FileUtil.writeUtf8String("v1", FileUtil.file(lib, "app.jar"));
        FileUtil.writeUtf8String("cfg", FileUtil.file(lib, "config/app.yml"));
        FileUtil.writeUtf8String("log", FileUtil.file(lib, "logs/run.log"));
        FileUtil.writeUtf8String("temp", FileUtil.file(lib, "temp/upload.part"));
        FileUtil.writeUtf8String("tmp", FileUtil.file(lib, "app.jar.tmp123"));
        return lib;
    }

    @Test
    public void testManifestIgnoresLogsTempAndTmpDir() throws Exception {
        File lib = newProjectDir();
        String manifest = ProjectFileVersionUtil.manifest(lib);
        Assert.assertNotNull(manifest);
        Assert.assertEquals(2, ProjectFileVersionUtil.countFiles(lib));
        // 内容不变时指纹必须稳定
        Assert.assertEquals(manifest, ProjectFileVersionUtil.manifest(lib));
        // logs 变化不影响指纹
        FileUtil.writeUtf8String("more log", FileUtil.file(lib, "logs/run.log"));
        FileUtil.writeUtf8String("temp2", FileUtil.file(lib, "temp/other.part"));
        Assert.assertEquals(manifest, ProjectFileVersionUtil.manifest(lib));
        // 业务文件变化必须影响指纹
        FileUtil.writeUtf8String("v2", FileUtil.file(lib, "app.jar"));
        Assert.assertNotEquals(manifest, ProjectFileVersionUtil.manifest(lib));
    }

    @Test
    public void testCopyDirContentSkipsIgnoreDirs() throws Exception {
        File lib = newProjectDir();
        File dest = temporaryFolder.newFolder("version-1");
        ProjectFileVersionUtil.copyDirContent(lib, dest);
        Assert.assertTrue(FileUtil.exist(FileUtil.file(dest, "app.jar")));
        Assert.assertTrue(FileUtil.exist(FileUtil.file(dest, "config/app.yml")));
        Assert.assertFalse(FileUtil.exist(FileUtil.file(dest, "logs")));
        Assert.assertFalse(FileUtil.exist(FileUtil.file(dest, "temp")));
        Assert.assertFalse(FileUtil.exist(FileUtil.file(dest, "app.jar.tmp123")));
        // 复制出来的内容指纹必须与源目录一致（否则切换后校验必定失败）
        Assert.assertEquals(ProjectFileVersionUtil.manifest(lib), ProjectFileVersionUtil.manifest(dest));
    }

    @Test
    public void testClearDirKeepsIgnoreDirsAndRemovesContent() throws Exception {
        File lib = newProjectDir();
        ProjectFileVersionUtil.clearDir(lib);
        Assert.assertFalse(FileUtil.exist(FileUtil.file(lib, "app.jar")));
        Assert.assertFalse(FileUtil.exist(FileUtil.file(lib, "config")));
        Assert.assertTrue(FileUtil.exist(FileUtil.file(lib, "logs/run.log")));
        Assert.assertTrue(FileUtil.exist(FileUtil.file(lib, "temp/upload.part")));
    }

    @Test
    public void testIsIgnore() {
        Assert.assertTrue(ProjectFileVersionUtil.isIgnore(new File("logs")));
        Assert.assertTrue(ProjectFileVersionUtil.isIgnore(new File("Temp")));
        Assert.assertTrue(ProjectFileVersionUtil.isIgnore(new File("app.jar.tmp1234")));
        Assert.assertFalse(ProjectFileVersionUtil.isIgnore(new File("app.jar")));
    }
}
