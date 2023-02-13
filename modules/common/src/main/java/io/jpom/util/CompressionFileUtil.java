/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.extractor.Extractor;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * 压缩文件工具
 *
 * @author bwcx_jzy
 */
@Slf4j
public class CompressionFileUtil {

    private static final Charset[] CHARSETS = new Charset[]{CharsetUtil.CHARSET_GBK, CharsetUtil.CHARSET_UTF_8};

    /**
     * 解压文件
     *
     * @param compressFile 压缩文件
     * @param destDir      解压到的文件夹
     */
    public static void unCompress(File compressFile, File destDir) {
        unCompress(compressFile, destDir, 0);
    }

    /**
     * 解压文件
     *
     * @param compressFile    压缩文件
     * @param destDir         解压到的文件夹
     * @param stripComponents 剔除文件夹
     */
    public static void unCompress(File compressFile, File destDir, int stripComponents) {
        try {
            unCompressTryCharset(compressFile, destDir, stripComponents);
        } catch (Exception e) {
            try (FileInputStream fileInputStream = new FileInputStream(compressFile);
                 CompressorInputStream compressUtilIn = CompressUtil.getIn(null, fileInputStream);) {
                if (compressUtilIn instanceof BZip2CompressorInputStream) {
                    File file = FileUtil.file(destDir, BZip2Utils.getUncompressedFilename(compressFile.getName()));
                    try (OutputStream outputStream = Files.newOutputStream(file.toPath())) {
                        IoUtil.copy(compressUtilIn, outputStream);
                    }
                } else {
                    unCompressByInputStreamTryCharset(compressFile, destDir, stripComponents);
                }
            } catch (Exception e2) {
                //
                e2.addSuppressed(e);
                //
                throw Lombok.sneakyThrow(e2);
            }
        }
    }

    private static void unCompressTryCharset(File compressFile, File destDir, int stripComponents) {
        for (int i = 0; i < CHARSETS.length; i++) {
            Charset charset = CHARSETS[i];
            try (Extractor extractor = CompressUtil.createExtractor(charset, compressFile)) {
                extractor.extract(destDir, stripComponents);
            } catch (Exception e) {
                log.warn("{} 解压异常 {} {}", compressFile.getName(), charset, e.getMessage());
                if (i == CHARSETS.length - 1) {
                    // 最后一个
                    throw Lombok.sneakyThrow(e);
                }
            }
        }
    }

    private static void unCompressByInputStreamTryCharset(File compressFile, File destDir, int stripComponents) {
        for (int i = 0; i < CHARSETS.length; i++) {
            Charset charset = CHARSETS[i];
            try (FileInputStream fileInputStream = new FileInputStream(compressFile);
                 CompressorInputStream compressUtilIn = CompressUtil.getIn(null, fileInputStream);) {
                try (Extractor extractor = CompressUtil.createExtractor(charset, compressUtilIn)) {
                    extractor.extract(destDir, stripComponents);
                }
            } catch (Exception e) {
                log.warn("解压异常 {} by InputStream {}", charset, e.getMessage());
                if (i == CHARSETS.length - 1) {
                    // 最后一个
                    throw Lombok.sneakyThrow(e);
                }
            }
        }
    }
}
