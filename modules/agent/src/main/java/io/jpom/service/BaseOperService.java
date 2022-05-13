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

import cn.hutool.core.util.ClassUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.BaseModel;
import io.jpom.util.JsonFileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 标准操作Service
 *
 * @author jiangzeyin
 * @since 2019/3/14
 */
public abstract class BaseOperService<T extends BaseModel> extends BaseDataService {

	private final String fileName;
	private final Class<?> typeArgument;

	public BaseOperService(String fileName) {
		this.fileName = fileName;
		this.typeArgument = ClassUtil.getTypeArgument(this.getClass());
	}

	/**
	 * 获取所有数据
	 *
	 * @return list
	 */
	public List<T> list() {
		return (List<T>) list(typeArgument);
	}

	public <E> List<E> list(Class<E> cls) {
		JSONObject jsonObject = getJSONObject();
		if (jsonObject == null) {
			return new ArrayList<>();
		}
		JSONArray jsonArray = JsonFileUtil.formatToArray(jsonObject);
		return jsonArray.toJavaList(cls);
	}

	public JSONObject getJSONObject() {
		Objects.requireNonNull(fileName, "没有配置fileName");
		return getJSONObject(fileName);
	}

	/**
	 * 工具id 获取 实体
	 *
	 * @param id 数据id
	 * @return T
	 */
	public T getItem(String id) {
		Objects.requireNonNull(fileName, "没有配置fileName");
		return (T) getJsonObjectById(fileName, id, typeArgument);
	}


	/**
	 * 添加实体
	 *
	 * @param t 实体
	 */
	public void addItem(T t) {
		Objects.requireNonNull(fileName, "没有配置fileName");
		saveJson(fileName, t);
	}

	/**
	 * 删除实体
	 *
	 * @param id 数据id
	 */
	public void deleteItem(String id) {
		Objects.requireNonNull(fileName, "没有配置fileName");
		deleteJson(fileName, id);
	}

	/**
	 * 修改实体
	 *
	 * @param t 实体
	 */
	public void updateItem(T t) {
		Objects.requireNonNull(fileName, "没有配置fileName");
		updateJson(fileName, t);
	}


}
