package io.jpom.util;

import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.extractor.Extractor;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

/**
 * 压缩文件工具
 *
 * @author bwcx_jzy
 */
public class CompressionFileUtil {

	public static void unCompress(File compressFile, File destDir) {
		Charset charset = CharsetDetector.detect(compressFile);
		charset = ObjectUtil.defaultIfNull(charset, CharsetUtil.defaultCharset());
		try {
			try (Extractor extractor = CompressUtil.createExtractor(charset, compressFile)) {
				extractor.extract(destDir);
			}
		} catch (Exception e) {
			CompressorInputStream compressUtilIn = null;
			try {
				compressUtilIn = CompressUtil.getIn(null, new FileInputStream(compressFile));
				try (Extractor extractor = CompressUtil.createExtractor(charset, compressUtilIn)) {
					extractor.extract(destDir);
				}
			} catch (Exception e2) {
				//
				e2.addSuppressed(e);
				//
				e2.printStackTrace();
			} finally {
				IOUtils.closeQuietly(compressUtilIn);
			}
		}
	}
}
