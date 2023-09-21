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
package org.dromara.jpom.transport;

import com.alibaba.fastjson2.TypeReference;

import java.util.function.Consumer;

/**
 * 插件端消息传输服务
 *
 * @author bwcx_jzy
 * @since 2022/12/18
 */
public interface TransportServer {

    /**
     * 请求 header
     */
    String WORKSPACE_ID_REQ_HEADER = "workspaceId";

    String JPOM_AGENT_AUTHORIZE = "Jpom-Agent-Authorize";

    String TRANSPORT_ENCRYPTION = "transport-encryption";

    /**
     * 执行请求
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求 item
     * @param data     参数
     * @return 响应的字符串
     */
    String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data);

    /**
     * 执行请求，返回响应的所有数据
     *
     * @param nodeInfo       节点信息
     * @param urlItem        请求 item
     * @param data           参数
     * @param tTypeReference 返回的泛型
     * @param <T>            泛型
     * @return 响应的字符串
     */
    default <T> T executeToType(INodeInfo nodeInfo, IUrlItem urlItem, Object data, TypeReference<T> tTypeReference) {
        String body = this.execute(nodeInfo, urlItem, data);
        return TransformServerFactory.get().transform(body, tTypeReference);
    }

    /**
     * 执行请求,仅返回成功的数据
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求 item
     * @param data     参数
     * @param tClass   返回的泛型
     * @param <T>      泛型
     * @return 响应的字符串
     */
    default <T> T executeToTypeOnlyData(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Class<T> tClass) {
        String body = this.execute(nodeInfo, urlItem, data);
        return TransformServerFactory.get().transformOnlyData(body, tClass);
    }

    /**
     * 下载文件
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求 item
     * @param data     参数
     * @param consumer 回调
     */
    void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer);

    /**
     * 创建 websocket 连接
     *
     * @param nodeInfo   节点信息
     * @param urlItem    请求 item
     * @param parameters 参数
     * @return websocket
     */
    IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters);

    /**
     * 传输方式
     * @param transportMode 0 服务器拉取，1 节点机器推送
     * @return true支持
     */
    boolean support(Integer transportMode);
}
