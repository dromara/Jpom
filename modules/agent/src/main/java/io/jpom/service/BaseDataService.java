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
package io.jpom.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.BaseModel;
import io.jpom.system.ConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.JsonFileUtil;

import java.io.FileNotFoundException;

/**
 * 公共文件操作Service
 *
 * @author jiangzeyin
 * @since 2019/1/16
 */
public abstract class BaseDataService {

	/**
	 * 获取数据文件的路径，如果文件不存在，则创建一个
	 *
	 * @param filename 文件名
	 * @return path
	 */
	protected String getDataFilePath(String filename) {
		return FileUtil.normalize(ConfigBean.getInstance().getDataPath() + StrUtil.SLASH + filename);
	}

	/**
	 * 保存json对象
	 *
	 * @param filename 文件名
	 * @param json     json数据
	 */
	protected void saveJson(String filename, BaseModel json) {
		String key = json.getId();
		// 读取文件，如果存在记录，则抛出异常
		JSONObject allData;
		JSONObject data = null;
		allData = getJSONObject(filename);
		if (allData != null) {
			data = allData.getJSONObject(key);
		} else {
			allData = new JSONObject();
		}
		// 判断是否存在数据
		if (null != data && 0 < data.keySet().size()) {
			throw new JpomRuntimeException("数据Id已经存在啦：" + filename + " :" + key);
		} else {
			allData.put(key, json.toJson());
			JsonFileUtil.saveJson(getDataFilePath(filename), allData);
		}
	}

	/**
	 * 修改json对象
	 *
	 * @param filename 文件名
	 * @param json     json数据
	 */
	protected void updateJson(String filename, BaseModel json) {
		String key = json.getId();
		// 读取文件，如果不存在记录，则抛出异常
		JSONObject allData = getJSONObject(filename);
		JSONObject data = allData.getJSONObject(key);

		// 判断是否存在数据
		if (null == data || 0 == data.keySet().size()) {
			throw new JpomRuntimeException("数据不存在:" + key);
		} else {
			allData.put(key, json.toJson());
			JsonFileUtil.saveJson(getDataFilePath(filename), allData);
		}
	}

	/**
	 * 删除json对象
	 *
	 * @param filename 文件
	 * @param key      key
	 */
	protected void deleteJson(String filename, String key) {
		// 读取文件，如果存在记录，则抛出异常
		JSONObject allData = getJSONObject(filename);
		if (allData == null) {
			return;
		}
		//Assert.notNull(allData, "没有任何数据");
		//JSONObject data = allData.getJSONObject(key);
		allData.remove(key);
		JsonFileUtil.saveJson(getDataFilePath(filename), allData);

	}

	/**
	 * 读取整个json文件
	 *
	 * @param filename 文件名
	 * @return json
	 */
	protected JSONObject getJSONObject(String filename) {
		try {
			return (JSONObject) JsonFileUtil.readJson(getDataFilePath(filename));
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	protected <T> T getJsonObjectById(String file, String id, Class<T> cls) {
		if (StrUtil.isEmpty(id)) {
			return null;
		}
		JSONObject jsonObject = getJSONObject(file);
		if (jsonObject == null) {
			return null;
		}
		jsonObject = jsonObject.getJSONObject(id);
		if (jsonObject == null) {
			return null;
		}
		return jsonObject.toJavaObject(cls);
	}
}
