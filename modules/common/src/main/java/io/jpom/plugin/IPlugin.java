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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ClassUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件模块接口
 *
 * @author bwcx_jzy
 * @since 2021/12/22
 */
public interface IPlugin extends AutoCloseable {

    /**
     * 执行插件方法
     *
     * @param main      拦截到到对象
     * @param parameter 执行方法传人的参数
     * @return 返回值
     * @throws Exception 异常
     */
    Object execute(Object main, Map<String, Object> parameter) throws Exception;

    /**
     * 执行插件方法
     *
     * @param main       主参数
     * @param parameters 其他参数
     * @return 结果
     * @throws Exception 异常
     */
    default Object execute(Object main, Object... parameters) throws Exception {
        // 处理参数
        int length = parameters.length;
        Map<String, Object> map = new HashMap<>(length / 2);
        for (int i = 0; i < length; i += 2) {
            map.put(parameters[i].toString(), parameters[i + 1]);
        }
        return this.execute(main, map);
    }

    /**
     * 执行插件方法
     *
     * @param main       拦截到到对象
     * @param parameters 其他参数
     * @param <T>        泛型
     * @param cls        返回值类型
     * @return 返回值
     * @throws Exception 异常
     */
    default <T> T execute(Object main, Class<T> cls, Object... parameters) throws Exception {
        Object execute = this.execute(main, parameters);
        return this.convertResult(execute, cls);
    }

    /**
     * 执行插件方法
     *
     * @param main      拦截到到对象
     * @param parameter 执行方法传人的参数
     * @param <T>       泛型
     * @param cls       返回值类型
     * @return 返回值
     * @throws Exception 异常
     */
    default <T> T execute(Object main, Map<String, Object> parameter, Class<T> cls) throws Exception {
        Object execute = this.execute(main, parameter);
        return this.convertResult(execute, cls);
    }

    /**
     * 转换结果
     *
     * @param execute 结果
     * @param cls     返回值类型
     * @param <T>     泛型
     * @return 返回值类型
     */
    @SuppressWarnings("unchecked")
    default <T> T convertResult(Object execute, Class<T> cls) {
        if (execute == null) {
            return null;
        }
        Class<?> aClass = execute.getClass();
        if (ClassUtil.isSimpleValueType(aClass)) {
            return (T) Convert.convert(aClass, execute);
        }
        // json 数据
        Object o = JSONObject.toJSON(execute);
        if (o instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) o;
            return jsonObject.toJavaObject(cls);
        }
        return (T) execute;
    }

    /**
     * 系统关闭，插件资源释放
     *
     * @throws Exception 异常
     */
    @Override
    default void close() throws Exception {
    }
}
