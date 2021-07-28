package io.jpom.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.charset.Charset;

/**
 * 外部资源配置
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Configuration
public class ExtConfigBean {

	public static final String FILE_NAME = "extConfig.yml";

	private static Resource resource;
	/**
	 * 请求日志
	 */
	@Value("${consoleLog.reqXss:true}")
	private boolean consoleLogReqXss;
	/**
	 * 请求响应
	 */
	@Value("${consoleLog.reqResponse:true}")
	private boolean consoleLogReqResponse;

	/**
	 * 日志文件的编码格式，如果没有指定就自动识别，自动识别可能出现不准确的情况
	 */
	@Value("${log.fileCharset:}")
	private String logFileCharset;
	/**
	 * 初始读取日志文件行号
	 */
	@Value("${log.intiReadLine:10}")
	private int logInitReadLine;
	/**
	 *
	 */
	private Charset logFileCharsets;

	public int getLogInitReadLine() {
		if (logInitReadLine < 0) {
			return 10;
		}
		return logInitReadLine;
	}

	public Charset getLogFileCharset() {
		return logFileCharsets;
	}

	public boolean isConsoleLogReqResponse() {
		return consoleLogReqResponse;
	}

	public boolean isConsoleLogReqXss() {
		return consoleLogReqXss;
	}

	private static ExtConfigBean extConfigBean;

	/**
	 * 动态获取外部配置文件的 resource
	 *
	 * @return File
	 */
	public static Resource getResource() {
		if (resource != null) {
			return resource;
		}
		File file = JpomManifest.getRunPath();
		if (file.isFile()) {
			file = file.getParentFile().getParentFile();
			file = new File(file, FILE_NAME);
			if (file.exists() && file.isFile()) {
				resource = new FileSystemResource(file);
				return ExtConfigBean.resource;
			}
		}
		resource = new ClassPathResource("/bin/" + FILE_NAME);
		return ExtConfigBean.resource;
	}

	public static File getResourceFile() {
		File file = JpomManifest.getRunPath();
		file = file.getParentFile().getParentFile();
		file = new File(file, FILE_NAME);
		return file;
	}

	/**
	 * 单例
	 *
	 * @return this
	 */
	public static ExtConfigBean getInstance() {
		if (extConfigBean == null) {
			extConfigBean = SpringUtil.getBean(ExtConfigBean.class);
			// 读取配置的编码格式
			if (StrUtil.isNotBlank(extConfigBean.logFileCharset)) {
				try {
					extConfigBean.logFileCharsets = CharsetUtil.charset(extConfigBean.logFileCharset);
				} catch (Exception ignored) {
				}
			}
		}
		return extConfigBean;
	}

	/**
	 * 项目运行存储路径
	 */
	@Value("${jpom.path}")
	private String path;

	public String getPath() {
		if (StrUtil.isEmpty(path)) {
			if (JpomManifest.getInstance().isDebug()) {
				// 调试模式 为根路径的 jpom文件
				path = ((SystemUtil.getOsInfo().isMac() ? "~" : "") + "/jpom/" + JpomApplication.getAppType().name() + "/").toLowerCase();
			} else {
				// 获取当前项目运行路径的父级
				File file = JpomManifest.getRunPath();
				if (!file.exists() && !file.isFile()) {
					throw new JpomRuntimeException("请配置运行路径属性【jpom.path】");
				}
				path = file.getParentFile().getParentFile().getAbsolutePath();
			}
		}
		return path;
	}

	public String getAbsolutePath() {
		return FileUtil.getAbsolutePath(FileUtil.file(getPath()));
	}
}
