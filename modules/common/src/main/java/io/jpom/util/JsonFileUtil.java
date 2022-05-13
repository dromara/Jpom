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
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.JpomRuntimeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * json 文件读写工具
 *
 * @author jiangzeyin
 * @since 2017/5/15
 */
public class JsonFileUtil {
	private static final ReentrantReadWriteLock FILE_LOCK = new ReentrantReadWriteLock();
	private final static ReentrantReadWriteLock.ReadLock READ_LOCK = FILE_LOCK.readLock();
	private final static ReentrantReadWriteLock.WriteLock WRITE_LOCK = FILE_LOCK.writeLock();

	/**
	 * 读取json 文件，同步
	 *
	 * @param path 路径
	 * @return JSON
	 * @throws FileNotFoundException 文件异常
	 */
	public static JSON readJson(String path) throws FileNotFoundException {
		File file = new File(path);
		if (!file.exists()) {
			throw new FileNotFoundException("没有找到对应配置文件：" + path);
		}
		READ_LOCK.lock();
		// 防止多线程操作文件异常
		try {
			String json = FileUtil.readString(file, CharsetUtil.CHARSET_UTF_8);
			if (StrUtil.isEmpty(json)) {
				return new JSONObject();
			}
			try {
				return (JSON) JSON.parse(json);
			} catch (Exception e) {
				throw new JpomRuntimeException("数据文件内容错误，请检查文件是否被非法修改：" + path, e);
			}
		} finally {
			READ_LOCK.unlock();
		}
	}

	/**
	 * 保存json 文件,同步
	 *
	 * @param path 路径
	 * @param json 新的json内容
	 */
	public static void saveJson(String path, JSON json) {
		WRITE_LOCK.lock();
		try {
			// 输出格式化后的json 字符串
			String newsJson = JSON.toJSONString(json, true);
			FileUtil.writeString(newsJson, path, CharsetUtil.UTF_8);
		} finally {
			WRITE_LOCK.unlock();
		}
	}

	public static <T> JSONObject arrayToObjById(JSONArray array) {
		JSONObject jsonObject = new JSONObject();
		array.forEach(o -> {
			JSONObject jsonObject1 = (JSONObject) o;
			jsonObject.put(jsonObject1.getString("id"), jsonObject1);
		});
		return jsonObject;
	}

	public static JSONArray formatToArray(JSONObject jsonObject) {
		if (jsonObject == null) {
			return new JSONArray();
		}
		Set<String> setKey = jsonObject.keySet();
		JSONArray jsonArray = new JSONArray();
		for (String key : setKey) {
			jsonArray.add(jsonObject.getJSONObject(key));
		}
		return jsonArray;
	}
}
