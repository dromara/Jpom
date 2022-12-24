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
package io.jpom.common.forward;

import com.alibaba.fastjson2.TypeReference;
import io.jpom.common.JsonMessage;
import top.jpom.transform.TransformServer;
import top.jpom.transport.INodeInfo;

/**
 * json 消息转换
 *
 * @author bwcx_jzy
 * @since 2022/12/24
 */
public class JsonMessageTransformServer implements TransformServer {

    @Override
    public <T> T transform(String data, TypeReference<T> tTypeReference) {
        return NodeForward.toJsonMessage(data, tTypeReference);
    }

    @Override
    public <T> T transformOnlyData(String data, Class<T> tClass) {
        JsonMessage<T> transform = this.transform(data, new TypeReference<JsonMessage<T>>() {
        });
        return transform.getData(tClass);
    }

    @Override
    public Exception transformException(Exception e, INodeInfo nodeInfo) {
        return NodeForward.responseException(e, nodeInfo);
    }
}
