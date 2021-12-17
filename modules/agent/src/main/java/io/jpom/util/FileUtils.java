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

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;

/**
 * 文件工具
 *
 * @author jiangzeyin
 * @date 2019/4/28
 */
public class FileUtils {

	private static JSONObject fileToJson(File file) {
		JSONObject jsonObject = new JSONObject(6);
		long sizeFile = FileUtil.size(file);
		if (file.isDirectory()) {
			jsonObject.put("isDirectory", true);
			jsonObject.put("fileSize", FileUtil.readableFileSize(sizeFile));
		} else {
			jsonObject.put("fileSize", FileUtil.readableFileSize(sizeFile));
		}
		jsonObject.put("filename", file.getName());
		long mTime = file.lastModified();
		jsonObject.put("modifyTimeLong", mTime);
		jsonObject.put("modifyTime", DateUtil.date(mTime).toString());
		return jsonObject;
	}

	/**
	 * 对文件信息解析排序
	 *
	 * @param files     文件数组
	 * @param time      是否安装时间排序
	 * @param startPath 开始路径
	 * @return 排序后的json
	 */
	public static JSONArray parseInfo(File[] files, boolean time, String startPath) {
		if (files == null) {
			return new JSONArray();
		}
		int size = files.length;
		JSONArray arrayFile = new JSONArray(size);
		for (File file : files) {
			JSONObject jsonObject = FileUtils.fileToJson(file);
			//
			if (startPath != null) {
				String levelName = StringUtil.delStartPath(file, startPath, false);
				jsonObject.put("levelName", levelName);
			}
			//
			arrayFile.add(jsonObject);
		}
		arrayFile.sort((o1, o2) -> {
			JSONObject jsonObject1 = (JSONObject) o1;
			JSONObject jsonObject2 = (JSONObject) o2;
			if (time) {
				return jsonObject2.getLong("modifyTimeLong").compareTo(jsonObject1.getLong("modifyTimeLong"));
			}
			return jsonObject1.getString("filename").compareTo(jsonObject2.getString("filename"));
		});
		final int[] i = {0};
		arrayFile.forEach(o -> {
			JSONObject jsonObject = (JSONObject) o;
			jsonObject.put("index", ++i[0]);
		});
		return arrayFile;
	}

	/**
	 * 判断路径是否满足jdk 条件
	 *
	 * @param path 路径
	 * @return 判断存在java文件
	 */
	public static boolean isJdkPath(String path) {
		String fileName = getJdkJavaPath(path, false);
		File newPath = new File(fileName);
		return newPath.exists() && newPath.isFile();
	}

	/**
	 * 获取java 文件路径
	 *
	 * @param path path
	 * @param w    是否使用javaw
	 * @return 完整路径
	 */
	public static String getJdkJavaPath(String path, boolean w) {
		String fileName;
		if (SystemUtil.getOsInfo().isWindows()) {
			fileName = w ? "javaw.exe" : "java.exe";
		} else {
			fileName = w ? "javaw" : "java";
		}
		File newPath = FileUtil.file(path, "bin", fileName);
		return FileUtil.getAbsolutePath(newPath);
	}

	/**
	 * 获取jdk 版本
	 *
	 * @param path jdk 路径
	 * @return 获取成功返回版本号
	 */
	public static String getJdkVersion(String path) {
		String newPath = getJdkJavaPath(path, false);
		if (path.contains(StrUtil.SPACE)) {
			newPath = String.format("\"%s\"", newPath);
		}
		String command = CommandUtil.execSystemCommand(newPath + "  -version");
		String[] split = StrUtil.splitToArray(command, StrUtil.LF);
		if (split == null || split.length <= 0) {
			return null;
		}
		String[] strings = StrUtil.splitToArray(split[0], "\"");
		if (strings == null || strings.length <= 1) {
			return null;
		}
		return strings[1];
	}
}
