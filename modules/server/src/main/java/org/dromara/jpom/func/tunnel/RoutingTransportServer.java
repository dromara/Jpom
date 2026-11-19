/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.DownloadCallback;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportAgentException;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannel;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannelRegistry;
import org.dromara.jpom.transport.tunnel.ws.TunnelDownload;
import org.dromara.jpom.transport.tunnel.ws.TunnelProxyWebSocket;
import org.dromara.jpom.transport.tunnel.ws.TunnelResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 传输路由器：节点有在线反向隧道就走隧道，否则回退到正向（HTTP 拉取）实现。
 * <p>
 * 正向行为零变化：只在 {@link INodeInfo#id()} 命中在线通道时才走隧道。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
public class RoutingTransportServer implements TransportServer {

    private final TunnelChannelRegistry registry;
    private final TransportServer forward;

    public RoutingTransportServer(TunnelChannelRegistry registry, TransportServer forward) {
        this.registry = registry;
        this.forward = forward;
    }

    @Override
    public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
        TunnelChannel channel = this.channel(nodeInfo);
        if (channel == null) {
            return forward.execute(nodeInfo, urlItem, data);
        }
        TunnelRequestBody requestBody = TunnelRequestBody.build(nodeInfo, urlItem, data);
        int timeout = timeout(urlItem);
        if (log.isDebugEnabled()) {
            log.debug("反向隧道请求 {}[{}] {} {}", nodeInfo.name(), urlItem.workspaceId(), requestBody.getContentType(), urlItem.path());
        }
        TunnelResponse response = channel.request(urlItem, requestBody.getBody(), requestBody.getContentType(), "POST", timeout,
            this.extraHeaders(nodeInfo, urlItem));
        int status = response.getStatus();
        if (status != 200) {
            log.warn("反向隧道响应错误 {} status={} body={}", nodeInfo.name(), status, response.bodyString());
            throw new TransportAgentException(nodeInfo.name() + " 节点通过反向隧道响应错误 status=" + status);
        }
        return response.bodyString();
    }

    @Override
    public void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer) {
        TunnelChannel channel = this.channel(nodeInfo);
        if (channel == null) {
            forward.download(nodeInfo, urlItem, data, consumer);
            return;
        }
        TunnelRequestBody requestBody = TunnelRequestBody.build(nodeInfo, urlItem, data);
        TunnelDownload download = channel.download(urlItem, requestBody.getBody(), requestBody.getContentType(), timeout(urlItem),
            this.extraHeaders(nodeInfo, urlItem));
        DownloadCallback callback = DownloadCallback.builder()
            .contentDisposition(download.getContentDisposition())
            .contentType(download.getContentType())
            .inputStream(download.getInputStream())
            .build();
        consumer.accept(callback);
    }

    @Override
    public IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters) {
        TunnelChannel channel = this.channel(nodeInfo);
        if (channel == null) {
            return forward.websocket(nodeInfo, urlItem, parameters);
        }
        // 反向模式下由服务端把节点授权信息随流打开参数下发，插件端无需读取自身授权摘要
        return TunnelProxyWebSocket.of(channel, urlItem, timeout(urlItem), this.extraHeaders(nodeInfo, urlItem), parameters);
    }

    /**
     * 附加请求头：节点授权信息与工作空间（与正向传输保持一致，插件端本机回环时直接沿用）
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求信息
     * @return 请求头
     */
    private Map<String, String> extraHeaders(INodeInfo nodeInfo, IUrlItem urlItem) {
        Map<String, String> headers = new HashMap<>();
        if (nodeInfo != null && nodeInfo.authorize() != null) {
            headers.put(org.dromara.jpom.transport.tunnel.TunnelConst.HEADER_AUTHORIZE, nodeInfo.authorize());
        }
        if (urlItem != null && urlItem.workspaceId() != null) {
            headers.put(org.dromara.jpom.transport.tunnel.TunnelConst.HEADER_WORKSPACE_ID, urlItem.workspaceId());
        }
        return headers;
    }

    private TunnelChannel channel(INodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return null;
        }
        return registry.get(nodeInfo.id());
    }

    private static int timeout(IUrlItem urlItem) {
        Integer timeout = urlItem == null ? null : urlItem.timeout();
        return timeout == null || timeout <= 0 ? 30 : timeout;
    }
}
