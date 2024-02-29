/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
}
