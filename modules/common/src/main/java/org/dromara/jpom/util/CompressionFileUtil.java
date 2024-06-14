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

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.extractor.Extractor;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

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
            try {
                unCompressByInputStreamTryCharset(compressFile, destDir, stripComponents);
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
                log.warn(I18nMessageUtil.get("i18n.unzip_exception.453e"), compressFile.getName(), charset, e.getMessage());
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
                log.warn(I18nMessageUtil.get("i18n.unzip_exception.92cc"), charset, e.getMessage());
                if (i == CHARSETS.length - 1) {
                    // 最后一个
                    throw Lombok.sneakyThrow(e);
                }
            }
        }
    }
}
