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
package io.jpom.common;

import cn.jiangzeyin.common.interceptor.DefaultHandlerMethodArgumentResolver;

/**
 * 解析 参数 url 编码
 *
 * @author bwcx_jzy
 * @since 2021/10/25
 */
public class UrlDecodeHandlerMethodArgumentResolver extends DefaultHandlerMethodArgumentResolver {

//	@Override
//	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
//		Object argument = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
//		if (argument instanceof String) {
//			// 解码
//			return URLUtil.decode(argument.toString());
//		} else if (argument instanceof BaseJsonModel) {
//			//	解码对象属性
//			Field[] fields = ReflectUtil.getFields(argument.getClass());
//			for (Field field : fields) {
//				Class<?> type = field.getType();
//				if (type == String.class) {
//					String fieldValue = (String) ReflectUtil.getFieldValue(argument, field);
//					fieldValue = URLUtil.decode(fieldValue);
//					ReflectUtil.setFieldValue(argument, field, fieldValue);
//				}
//			}
//		}
//		return argument;
//	}
}
