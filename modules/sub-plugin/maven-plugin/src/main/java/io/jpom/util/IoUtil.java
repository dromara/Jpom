package io.jpom.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

/**
 * @author bwcx_jzy
 * @since 2021/12/24
 */
public class IoUtil {

	/**
	 * 日志输出
	 */
	private static final Log log = new SystemStreamLog();

	/**
	 * 关闭流
	 *
	 * @param autoCloseable 自动关闭流接口
	 */
	public static void close(AutoCloseable autoCloseable) {
		if (autoCloseable == null) {
			return;
		}
		try {
			autoCloseable.close();
		} catch (Exception e) {
			log.error("io close", e);
		}
	}
}
