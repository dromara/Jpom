//package io.jpom.build;
//
//import cn.hutool.core.collection.CollectionUtil;
//import cn.hutool.core.exceptions.ExceptionUtil;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.io.file.FileWriter;
//import cn.hutool.core.util.CharsetUtil;
//import cn.jiangzeyin.common.DefaultSystemLog;
//import lombok.Builder;
//
//import java.io.File;
//import java.io.PrintWriter;
//import java.nio.charset.Charset;
//
///**
// * 日志记录
// *
// * @author bwcx_jzy
// * @since 2022/1/26
// */
//@Builder
//public class LogRecorder {
//	/**
//	 * 文件
//	 */
//	private final File file;
//	/**
//	 * 文件编码
//	 */
//	private final Charset charset = CharsetUtil.CHARSET_UTF_8;
//
//	/**
//	 * 记录错误信息
//	 *
//	 * @param title     错误描述
//	 * @param throwable 堆栈信息
//	 */
//	public void log(String title, Throwable throwable) {
//		DefaultSystemLog.getLog().error(title, throwable);
//		FileUtil.appendLines(CollectionUtil.toList(title), this.file, this.charset);
//		String s = ExceptionUtil.stacktraceToString(throwable);
//		FileUtil.appendLines(CollectionUtil.toList(s), this.file, this.charset);
//	}
//
//	/**
//	 * 记录单行日志
//	 *
//	 * @param info 日志
//	 */
//	public void log(String info) {
//		FileUtil.appendLines(CollectionUtil.toList(info), this.file, this.charset);
//	}
//
//	/**
//	 * 获取 文件输出流
//	 *
//	 * @return Writer
//	 */
//	public PrintWriter getPrintWriter() {
//		return FileWriter.create(this.file, this.charset).getPrintWriter(true);
//	}
//}
