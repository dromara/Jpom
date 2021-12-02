/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson.JSON;

import java.io.File;

/**
 * main 方法运行参数工具
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class StringUtil {

	/**
	 * 支持的压缩包格式
	 */
	public static final String[] PACKAGE_EXT = new String[]{"tar.bz2", "tar.gz", "tar", "bz2", "zip", "gz"};

	/**
	 * 获取启动参数
	 *
	 * @param args 所有参数
	 * @param name 参数名
	 * @return 值
	 */
	public static String getArgsValue(String[] args, String name) {
		if (args == null) {
			return null;
		}
		for (String item : args) {
			item = StrUtil.trim(item);
			if (item.startsWith("--" + name + "=")) {
				return item.substring(name.length() + 3);
			}
		}
		return null;
	}

	/**
	 * id输入规则
	 *
	 * @param value 值
	 * @param min   最短
	 * @param max   最长
	 * @return true
	 */
	public static boolean isGeneral(CharSequence value, int min, int max) {
		String reg = "^[a-zA-Z0-9_-]{" + min + "," + max + "}$";
		return Validator.isMatchRegex(reg, value);
	}

	/**
	 * 删除文件开始的路径
	 *
	 * @param file      要删除的文件
	 * @param startPath 开始的路径
	 * @param inName    是否返回文件名
	 * @return /test/a.txt /test/  a.txt
	 */
	public static String delStartPath(File file, String startPath, boolean inName) {
		String newWhitePath;
		if (inName) {
			newWhitePath = FileUtil.getAbsolutePath(file.getAbsolutePath());
		} else {
			newWhitePath = FileUtil.getAbsolutePath(file.getParentFile());
		}
		String itemAbsPath = FileUtil.getAbsolutePath(new File(startPath));
		itemAbsPath = FileUtil.normalize(itemAbsPath);
		newWhitePath = FileUtil.normalize(newWhitePath);
		String path = StrUtil.removePrefix(newWhitePath, itemAbsPath);
		//newWhitePath.substring(newWhitePath.indexOf(itemAbsPath) + itemAbsPath.length());
		path = FileUtil.normalize(path);
		if (path.startsWith(StrUtil.SLASH)) {
			path = path.substring(1);
		}
		return path;
	}

	/**
	 * 获取jdk 中的tools jar文件路径
	 *
	 * @return file
	 */
	public static File getToolsJar() {
		File file = new File(SystemUtil.getJavaRuntimeInfo().getHomeDir());
		return new File(file.getParentFile(), "lib/tools.jar");
	}

	/**
	 * 指定时间的下一个刻度
	 *
	 * @return String
	 */
	public static String getNextScaleTime(String time, Long millis) {
		DateTime dateTime = DateUtil.parse(time);
		if (millis == null) {
			millis = 30 * 1000L;
		}
		DateTime newTime = dateTime.offsetNew(DateField.SECOND, (int) (millis / 1000));
		return DateUtil.formatTime(newTime);
	}

//	/**
//	 * 删除 yml 文件内容注释
//	 *
//	 * @param content 配置内容
//	 * @return 移除后的内容
//	 */
//	public static String deleteComment(String content) {
//		List<String> split = StrUtil.split(content, StrUtil.LF);
//		split = split.stream().filter(s -> {
//			if (StrUtil.isEmpty(s)) {
//				return false;
//			}
//			s = StrUtil.trim(s);
//			return !StrUtil.startWith(s, "#");
//		}).collect(Collectors.toList());
//		return CollUtil.join(split, StrUtil.LF);
//	}

	/**
	 * json 字符串转 bean，兼容普通json和字符串包裹情况
	 *
	 * @param jsonStr json 字符串
	 * @param cls     要转为bean的类
	 * @param <T>     泛型
	 * @return data
	 */
	public static <T> T jsonConvert(String jsonStr, Class<T> cls) {
		try {
			return JSON.parseObject(jsonStr, cls);
		} catch (Exception e) {
			return JSON.parseObject(JSON.parse(jsonStr).toString(), cls);
		}
	}
}
