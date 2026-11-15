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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.MessageSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link BaseJpomController#uploadSharding} 分片上传扩展名校验行为测试。
 *
 * <p>背景：在线升级发布包 2.11.13.2 起提供 tar.gz（过渡期同时有 zip），手工上传时
 * web 端分片文件名为 {@code server-x-release.tar.gz.0}（序号后缀在最后一段），
 * {@code FileUtil.extName} 只能取到 {@code gz}，因此需要按复合扩展名匹配 tar.gz，
 * 同时不能放宽到任意 {@code .jar.zip} 之类的组合（保持原有安全校验）。
 *
 * @author bwcx_jzy
 */
public class UploadShardingExtensionTest {

    private static final String[] PACKAGE_EXT_NAMES = {"jar", "zip", "tar.gz"};

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
    public void testTarGzChunkAllowed() throws IOException {
        assertUploadAllowed("server-9.9.9-release.tar.gz.0", "slice-tar-gz");
    }

    @Test
    public void testUpperCaseTarGzChunkAllowed() throws IOException {
        assertUploadAllowed("server-9.9.9-release.TAR.GZ.0", "slice-tar-gz-upper");
    }

    @Test
    public void testZipChunkAllowed() throws IOException {
        assertUploadAllowed("server-9.9.9-release.zip.0", "slice-zip");
    }

    @Test
    public void testJarChunkAllowed() throws IOException {
        assertUploadAllowed("server-9.9.9.jar.0", "slice-jar");
    }

    @Test
    public void testCompoundExtensionNotLoosened() throws IOException {
        // .jar.zip 的真实扩展名是 zip，不能因为名称里出现 jar 就被放行
        assertUploadRejected("server-9.9.9.jar.zip.0", new String[]{"jar"}, "slice-jar-zip");
    }

    @Test
    public void testOtherExtensionRejected() throws IOException {
        assertUploadRejected("server-9.9.9-release.exe.0", PACKAGE_EXT_NAMES, "slice-exe");
    }

    private void assertUploadAllowed(String originalFilename, String sliceId) throws IOException {
        File tempPath = temporaryFolder.newFolder(sliceId + "-temp");
        new TestController().uploadSharding(new FakeMultipartFile(originalFilename), tempPath.getAbsolutePath(),
            sliceId, 1, 0, "file-sum-md5", PACKAGE_EXT_NAMES);
        File sliceFile = new File(tempPath, "slice/" + sliceId + "/items/" + originalFilename);
        assertTrue(sliceFile.isFile());
    }

    private void assertUploadRejected(String originalFilename, String[] extNames, String sliceId) throws IOException {
        File tempPath = temporaryFolder.newFolder(sliceId + "-temp");
        try {
            new TestController().uploadSharding(new FakeMultipartFile(originalFilename), tempPath.getAbsolutePath(),
                sliceId, 1, 0, "file-sum-md5", extNames);
            fail("不支持的扩展名应被拒绝：" + originalFilename);
        } catch (IllegalStateException e) {
            assertTrue("错误提示不应为空", !e.getMessage().isEmpty());
        }
    }

    /**
     * BaseJpomController 是抽象类但没有抽象方法，直接匿名/内部子类实例化即可
     */
    private static class TestController extends BaseJpomController {
    }

    private static class FakeMultipartFile implements MultipartFile {
        private final String originalFilename;

        private FakeMultipartFile(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            return new byte[0];
        }

        @Override
        public InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(new byte[0]);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), "chunk".getBytes(StandardCharsets.UTF_8));
        }
    }
}
