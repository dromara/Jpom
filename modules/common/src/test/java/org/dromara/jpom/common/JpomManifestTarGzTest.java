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

import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.Type;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticMessageSource;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link JpomManifest#zipFileFind(String, Type, String)} 发布包解析行为级测试。
 *
 * <p>背景：2.11.13.2 起发布包改为 tar.gz（过渡期同时提供 zip），在线升级需要从
 * tar.gz/.tgz 中提取 {@code lib/<type>-<version>.jar}；本测试覆盖 tar.gz 提取、
 * 同包多 jar 的类型过滤、./ 前缀兼容、zip/jar 历史行为不回退以及非法包报错。
 *
 * @author bwcx_jzy
 */
public class JpomManifestTarGzTest {

    private static final int TAR_BLOCK_SIZE = 512;

    private static final byte[] JAR_CONTENT = "fake-jar-content".getBytes(StandardCharsets.UTF_8);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * I18nMessageUtil 的 MessageSource 为懒加载，非法包报错会用到；这里初始化一个
     * 最小 Spring 上下文并用 code 兜底，避免单测环境缺少 MessageSource 而报无关错误。
     */
    @BeforeClass
    public static void initI18nContext() {
        if (SpringUtil.getApplicationContext() != null) {
            return;
        }
        GenericApplicationContext context = new GenericApplicationContext();
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.setUseCodeAsDefaultMessage(true);
        context.registerBean("messageSource", MessageSource.class, () -> messageSource);
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    @Test
    public void testTarGzFindServerJar() throws IOException {
        File packageFile = temporaryFolder.newFile("server-9.9.9-release.tar.gz");
        writeTarGz(packageFile,
            "./conf/application.yml",
            "./lib/agent-9.9.9.jar",
            "./lib/server-9.9.9.jar",
            "./bin/Server.sh");
        File savePath = temporaryFolder.newFolder("save-tar-gz");
        File jarFile = JpomManifest.zipFileFind(packageFile.getAbsolutePath(), Type.Server, savePath.getAbsolutePath());
        assertEquals("server-9.9.9.jar", jarFile.getName());
        assertEquals(savePath.getAbsolutePath(), jarFile.getAbsoluteFile().getParent());
        assertArrayEquals(JAR_CONTENT, Files.readAllBytes(jarFile.toPath()));
    }

    @Test
    public void testTgzFindAgentJarWithoutDotPrefix() throws IOException {
        File packageFile = temporaryFolder.newFile("agent-9.9.9-release.tgz");
        writeTarGz(packageFile,
            "conf/application.yml",
            "lib/agent-9.9.9.jar",
            "bin/Agent.sh");
        File savePath = temporaryFolder.newFolder("save-tgz");
        File jarFile = JpomManifest.zipFileFind(packageFile.getAbsolutePath(), Type.Agent, savePath.getAbsolutePath());
        assertEquals("agent-9.9.9.jar", jarFile.getName());
        assertArrayEquals(JAR_CONTENT, Files.readAllBytes(jarFile.toPath()));
    }

    @Test
    public void testTarGzWithoutTargetJarThrows() throws IOException {
        File packageFile = temporaryFolder.newFile("server-invalid-release.tar.gz");
        writeTarGz(packageFile, "./conf/application.yml", "./lib/agent-9.9.9.jar");
        File savePath = temporaryFolder.newFolder("save-invalid-tar-gz");
        try {
            JpomManifest.zipFileFind(packageFile.getAbsolutePath(), Type.Server, savePath.getAbsolutePath());
            fail("tar.gz 中没有目标 jar 时应抛出异常");
        } catch (IllegalStateException e) {
            assertFalse(e.getMessage().isEmpty());
        }
    }

    @Test
    public void testZipFindStillWorks() throws IOException {
        File packageFile = temporaryFolder.newFile("server-8.8.8-release.zip");
        writeZip(packageFile, "conf/application.yml", "lib/server-8.8.8.jar");
        File savePath = temporaryFolder.newFolder("save-zip");
        File jarFile = JpomManifest.zipFileFind(packageFile.getAbsolutePath(), Type.Server, savePath.getAbsolutePath());
        assertEquals("server-8.8.8.jar", jarFile.getName());
        assertArrayEquals(JAR_CONTENT, Files.readAllBytes(jarFile.toPath()));
    }

    @Test
    public void testJarFileReturnedDirectly() throws IOException {
        File jarFile = temporaryFolder.newFile("server-9.9.9.jar");
        Files.write(jarFile.toPath(), JAR_CONTENT);
        File result = JpomManifest.zipFileFind(jarFile.getAbsolutePath(), Type.Server,
            temporaryFolder.newFolder("save-jar").getAbsolutePath());
        assertTrue(result.isFile());
        assertEquals(jarFile.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    public void testUnsupportedPackageThrows() throws IOException {
        File packageFile = temporaryFolder.newFile("server-9.9.9-release.rar");
        Files.write(packageFile.toPath(), JAR_CONTENT);
        try {
            JpomManifest.zipFileFind(packageFile.getAbsolutePath(), Type.Server,
                temporaryFolder.newFolder("save-rar").getAbsolutePath());
            fail("不支持的安装包扩展名应抛出异常");
        } catch (IllegalArgumentException e) {
            assertFalse(e.getMessage().isEmpty());
        }
    }

    private void writeTarGz(File packageFile, String... entryNames) throws IOException {
        try (OutputStream fileStream = Files.newOutputStream(packageFile.toPath());
             GzipCompressorOutputStream gzipStream = new GzipCompressorOutputStream(fileStream)) {
            for (String entryName : entryNames) {
                byte[] content = entryContent(entryName);
                gzipStream.write(buildTarHeader(entryName, content.length));
                gzipStream.write(content);
                int padding = (TAR_BLOCK_SIZE - content.length % TAR_BLOCK_SIZE) % TAR_BLOCK_SIZE;
                if (padding > 0) {
                    gzipStream.write(new byte[padding]);
                }
            }
            // 两个 512 字节全零块表示 tar 归档结束
            gzipStream.write(new byte[TAR_BLOCK_SIZE * 2]);
        }
    }

    /**
     * 手工构造 ustar 普通文件头。
     *
     * <p>commons-compress 的 TarArchiveOutputStream 依赖可选包 commons-codec，common 模块
     * 未引入该依赖；这里手工写 tar 头，既避免为单测新增依赖，也确保测试 classpath 与生产
     * 运行时（只读 tar.gz）一致。
     */
    private static byte[] buildTarHeader(String entryName, int contentLength) {
        byte[] header = new byte[TAR_BLOCK_SIZE];
        byte[] nameBytes = entryName.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, header, 0, nameBytes.length);
        // mode 0644、uid/gid 0、size（11 位八进制 + NUL）、mtime
        writeAscii(header, 100, "0000644");
        writeAscii(header, 108, "0000000");
        writeAscii(header, 116, "0000000");
        writeAscii(header, 124, String.format("%011o", contentLength));
        writeAscii(header, 136, "00000000000");
        // 校验和字段先填空格，求和后再写入
        for (int i = 148; i < 156; i++) {
            header[i] = ' ';
        }
        // typeflag：普通文件；magic ustar + version 00
        header[156] = '0';
        System.arraycopy("ustar".getBytes(StandardCharsets.US_ASCII), 0, header, 257, 5);
        header[263] = '0';
        header[264] = '0';
        int checksum = 0;
        for (byte b : header) {
            checksum += b & 0xff;
        }
        writeAscii(header, 148, String.format("%06o", checksum));
        header[155] = ' ';
        return header;
    }

    private static void writeAscii(byte[] header, int offset, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(bytes, 0, header, offset, bytes.length);
    }

    private void writeZip(File packageFile, String... entryNames) throws IOException {
        try (OutputStream fileStream = Files.newOutputStream(packageFile.toPath());
             ZipOutputStream zipStream = new ZipOutputStream(fileStream)) {
            for (String entryName : entryNames) {
                byte[] content = entryContent(entryName);
                zipStream.putNextEntry(new ZipEntry(entryName));
                zipStream.write(content);
                zipStream.closeEntry();
            }
        }
    }

    private static byte[] entryContent(String entryName) {
        if (entryName.endsWith(".jar")) {
            return JAR_CONTENT;
        }
        return ("content-of-" + entryName).getBytes(StandardCharsets.UTF_8);
    }
}
