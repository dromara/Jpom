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
package io.jpom.plugin;

import java.util.Map;

/**
 * 插件模块接口
 *
 * @author bwcx_jzy
 * @since 2021/12/22
 */
public interface IPlugin {

	/**
	 * 执行插件方法
	 *
	 * @param main      拦截到到对象
	 * @param parameter 执行方法传人的参数
	 * @return 返回值
	 */
	Object execute(Object main, Map<String, Object> parameter);

	/**
	 * 做一些检查
	 *
	 * @param type      检查的类型
	 * @param main      拦截到到对象
	 * @param parameter 执行方法传人的参数
	 * @return true 检测通过
	 */
	default boolean check(String type, Object main, Map<String, Object> parameter) {
		throw new RuntimeException("Not implements");
	}

	/**
	 * 插件的名字
	 *
	 * @return 名称
	 */
	String name();

	/**
	 * 插件父级
	 *
	 * @return 父级名称
	 */
	default String parent() {
		return null;
	}

	/**
	 * 排序值
	 *
	 * @return 值越小，排到前面 正序
	 */
	default int order() {
		return Integer.MIN_VALUE;
	}
}
