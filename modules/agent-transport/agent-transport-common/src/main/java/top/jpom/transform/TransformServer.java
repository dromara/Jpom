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
package top.jpom.transform;

import com.alibaba.fastjson2.TypeReference;
import top.jpom.transport.INodeInfo;

/**
 * 消息转换服务
 *
 * @author bwcx_jzy
 * @since 2022/12/24
 */
public interface TransformServer {

    /**
     * 数据类型转换
     *
     * @param data           数据
     * @param tTypeReference 类型
     * @param <T>            范型
     * @return data
     */
    <T> T transform(String data, TypeReference<T> tTypeReference);

    /**
     * 数据类型转换,只返回成功的数据
     *
     * @param data   数据
     * @param tClass 类型
     * @param <T>    范型
     * @return data
     */
    <T> T transformOnlyData(String data, Class<T> tClass);

    /**
     * 转换异常
     *
     * @param e        请求的异常
     * @param nodeInfo 节点信息
     * @return 转换后的异常
     */
    default Exception transformException(Exception e, INodeInfo nodeInfo) {
        return e;
    }
}
